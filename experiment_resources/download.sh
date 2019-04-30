#!/bin/bash

ROOT_DIRECTORY="$(pwd)"

# the list of GitHub repositories built by maven
project_list="${ROOT_DIRECTORY}/work_list.dat"

# the destination path to the downloaded projects
dest_dir="${ROOT_DIRECTORY}/sample-projects"

# check whether the dest folder exists first
if [ ! -d "$dest_dir" ]; then
	mkdir $dest_dir
fi

printf "***************Start downloading the given GitHub projects**********************\n"

while read line
do
	#echo "$line"
	if [[ $line != *"_"* ]]; then
		# incorrect repo name
		continue
	fi
	line=`echo $line | awk -F'\r' '{print $1}'`
	username=`echo $line | awk -F'_' '{print $1}'`
	reponame=`echo $line | awk -F'_' '{print $2}'`
	#echo "Name=${username}; Repo=${reponame}"
	project="${username}_${reponame}"

	if [ ! -d "${dest_dir}/${project}" ]; then
		mkdir "${dest_dir}/${project}"
		printf "Beginning to clone $line\n"
		`git clone "https://github.com/${username}/${reponame}.git" "${dest_dir}/${project}" > /dev/null 2>&1` 
		printf "Successfully cloned ${line}!\n\n"
	else
		printf "$line already cloned. Skipp it.\n"
	fi

	#Checkout to a particular date (this keeps experiments constant across time)
	cd ${dest_dir}/${project}
    	current_branch=$(git branch | grep \* | cut -d ' ' -f2)
	git checkout `git rev-list -n 1 --before="2018-10-15 12:00" ${current_branch}` >/dev/null 2>&1
	cd ${ROOT_DIRECTORY}

done < $project_list

# "dieforfree_qart4j" has a unique "pom.xml" file that results in the apache-commons-imaging library being
# fetched whenever maven is run. This caused problems when running experiments. I therefore modify the
# project's "pom.xml" file to use a local version of the apache-commons-imaging library.

if [ -d "sample-projects/dieforfree_qart4j" ]; then
	cp -r qart4j_patch/* sample-projects/dieforfree_qart4j/
fi
