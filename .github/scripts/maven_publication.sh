#!/usr/bin/env bash
# 查询 wintercogs Maven 里 gradle.properties 当前版本的发布是否齐全，供 maven-publish 工作流使用。
# 一次发布包括模组 jar、源码包、POM 与 Gradle 模块元数据，每个文件各带一个 SHA-256 校验文件。
#   plan      一个都没有时输出 publish=true，全都有时输出 publish=false，只有一部分时失败
#   complete  缺任何一个文件就失败
set -euo pipefail

mode="${1:?usage: maven_publication.sh plan|complete}"
repository="${MAVEN_RELEASES:-https://maven.wintercogs.com/releases}"

property() {
    sed -n "s/^[[:space:]]*$1[[:space:]]*=[[:space:]]*//p" gradle.properties | head -n 1 | tr -d '\r'
}

group="$(property mod_group_id)"
mod_id="$(property mod_id)"
minecraft_version="$(property minecraft_version)"
version="$(property mod_version)"
if [[ -z "$group" || -z "$mod_id" || -z "$minecraft_version" || -z "$version" ]]; then
    echo "::error::mod_group_id, mod_id, minecraft_version or mod_version is missing from gradle.properties"
    exit 1
fi
# 与 build.gradle 里 base.archivesName 相同，发布时用作 artifactId
artifact="$mod_id-$minecraft_version-forge"
base="$repository/${group//.//}/$artifact/$version"

present=()
missing=()
body="$(mktemp)"
trap 'rm -f "$body"' EXIT
for name in "$artifact-$version.jar" "$artifact-$version-sources.jar" "$artifact-$version.pom" "$artifact-$version.module"; do
    for file in "$name" "$name.sha256"; do
        # 网络出错时直接失败，不能当作文件不存在；校验文件要取回内容确认格式
        if [[ "$file" == *.sha256 ]]; then
            status="$(curl --silent --show-error --retry 2 --output "$body" --write-out '%{http_code}' "$base/$file")"
        else
            status="$(curl --silent --show-error --retry 2 --head --output /dev/null --write-out '%{http_code}' "$base/$file")"
        fi
        case "$status" in
            200)
                if [[ "$file" == *.sha256 ]] && ! grep -qiE '^[0-9a-f]{64}[[:space:]]*$' "$body"; then
                    echo "::error::Invalid SHA-256 file $base/$file. Inspect this version in Reposilite before retrying."
                    exit 1
                fi
                present+=("$file")
                ;;
            404) missing+=("$file") ;;
            *)
                echo "::error::Cannot check $base/$file: HTTP $status"
                exit 1
                ;;
        esac
    done
done

case "$mode" in
    plan)
        if [[ ${#present[@]} -eq 0 ]]; then
            echo "The repository does not have $artifact $version yet; it will be published."
            echo "publish=true" >> "$GITHUB_OUTPUT"
        elif [[ ${#missing[@]} -eq 0 ]]; then
            echo "::notice title=Already published::$artifact $version is already in the repository; nothing to publish."
            echo "publish=false" >> "$GITHUB_OUTPUT"
        else
            echo "::error::$artifact $version is only partly in the repository (missing: ${missing[*]}). Remove this version in Reposilite before retrying."
            exit 1
        fi
        ;;
    complete)
        if [[ ${#missing[@]} -ne 0 ]]; then
            echo "::error::$artifact $version is incomplete in the repository (missing: ${missing[*]})."
            exit 1
        fi
        echo "$artifact $version is complete in the repository."
        ;;
    *)
        echo "::error::Unknown mode: $mode"
        exit 1
        ;;
esac
