#!/bin/bash
# chmod +x dev_sentinel_build_deploy_yosemite_explorer_backend.sh

args=("$@")

#PLAY_APP_SERVER_NAME="${args[0]}"
PLAY_APP_SERVER_NAME="testnet-sentinel-explorer-backend-server"
BASEDIR=$(dirname "$0")

PLAY_APP_NAME="yosemite-explorer-backend"
PLAY_APP_VERSION="1.0-SNAPSHOT"

red=`tput setaf 1`
green=`tput setaf 2`
magenta=`tput setaf 6`
reset=`tput sgr0`

PLAY_BASE_PATH="${BASEDIR}/.."
PLAY_APP_DIST_FILE_NAME="${PLAY_APP_NAME}-${PLAY_APP_VERSION}.zip"
PLAY_APP_DIST_FILE="${PLAY_BASE_PATH}/target/universal/${PLAY_APP_DIST_FILE_NAME}"
RUN_PLAY_APP_SERVERSIDE_SCRIPT="${PLAY_BASE_PATH}/deploy/dev_sentinel_run_yosemite_explorer_backend_serverside.sh"
SERVER_SSH_KEY_FILE="${PLAY_BASE_PATH}/../AWS/ssh_key/ysmt_sentinel_testnet_dev_server_ap_northeast_seoul.pem"
SERVER_ADDRESS="testnet-sentinel-explorer-api.yosemitelabs.org"
SERVER_USER_HOST="ubuntu@${SERVER_ADDRESS}"
PLAY_APP_SERVER_TYPE="${red}[${PLAY_APP_NAME}]${green}[DevelopmentServer]${magenta}[${PLAY_APP_SERVER_NAME}]${reset}"

echo "${green}PLAY_BASE_PATH${reset}=${red}$PLAY_BASE_PATH${reset}"
echo "${green}PLAY_APP_DIST_FILE${reset}=${red}$PLAY_APP_DIST_FILE${reset}"
echo "${green}SERVER_ADDRESS${reset}=${red}$SERVER_ADDRESS${reset}"
echo "${green}PLAY_APP_SERVER_TYPE${reset}=${red}$PLAY_APP_SERVER_TYPE${reset}"

echo "${red}Do you want to deploy ${PLAY_APP_SERVER_TYPE}?${reset}"
echo "write YES to proceed deploy process"
read USER_CONFIRM_TO_PROCEED
if [ "$USER_CONFIRM_TO_PROCEED" != "YES" ]; then
  exit 1
fi

echo ${PLAY_APP_SERVER_TYPE}

cd $PLAY_BASE_PATH

if [[ "${args[1]}" == "--noclean" ]]; then
    sbt update compile dist
else
    sbt clean update compile dist
fi

MD5_HASH=`md5 ${PLAY_APP_DIST_FILE} | awk -F' = ' '{print $2}'`

echo $PLAY_APP_SERVER_TYPE "MD5_HASH(${PLAY_APP_DIST_FILE_NAME}) =" ${MD5_HASH}

scp -i ${SERVER_SSH_KEY_FILE} ${PLAY_APP_DIST_FILE} ${SERVER_USER_HOST}:/mnt/${PLAY_APP_DIST_FILE_NAME}

MD5_HASH_SERVER=`ssh -i ${SERVER_SSH_KEY_FILE} ${SERVER_USER_HOST} md5sum /mnt/${PLAY_APP_DIST_FILE_NAME} | awk '{print $1}'`

echo $PLAY_APP_SERVER_TYPE "MD5_HASH_SERVER(${PLAY_APP_DIST_FILE_NAME}) =" $MD5_HASH_SERVER

if [ $MD5_HASH = $MD5_HASH_SERVER ]; then
	echo $PLAY_APP_SERVER_TYPE 'MD5_HASH == MD5_HASH_SERVER'
else
	echo $PLAY_APP_SERVER_TYPE 'MD5_HASH != MD5_HASH_SERVER'
	exit 0
fi

ssh -i ${SERVER_SSH_KEY_FILE} ${SERVER_USER_HOST} 'bash -s' < ${RUN_PLAY_APP_SERVERSIDE_SCRIPT}

echo $PLAY_APP_SERVER_TYPE Finished

echo $PLAY_APP_SERVER_TYPE Waiting.. and launch index page...
sleep 10

open "http://${SERVER_ADDRESS}"

