#!/bin/bash -e

# This script is intended to be invoked from boot in the
# jar subdirectory.  Use the command:
#
# $ boot docs publish
#
# This assumes that you have your GitHub credentials setup
# within your environment.  Note that publishing with no
# changes will fail and will leave you on the "gh-pages"
# branch.

# everything must be done at the root of the repository
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
echo ${DIR}
cd ${DIR}

# checkout the published GitHub Pages branch
git checkout gh-pages

# remove all current files (except .gitignore)
git ls-files -z | xargs -0 rm -f
git reset HEAD .gitignore
git checkout .gitignore

# copy generated documentation into place
cp -r jar/target/doc/* .

# add all of the changes, commit, push
git add -A
git commit -m "update docs"
git push

# come back to the master branch
git checkout master
