#!/bin/bash

PLAY_APP_NAME="yosemite-explorer-backend"
PLAY_APP_NAME_LOWER=`echo ${PLAY_APP_NAME} | tr "[:upper:]" "[:lower:]"`
PLAY_APP_VERSION="1.0-SNAPSHOT"
PLAY_APP_DIST_NAME="${PLAY_APP_NAME}-${PLAY_APP_VERSION}"
PLAY_APP_DIST_NAME_LOWER="${PLAY_APP_NAME_LOWER}-${PLAY_APP_VERSION}"
PLAY_APP_CONFIG_FILE="application.dev.conf"
PLAY_APP_LOGGER_FILE="logback.dev.xml"


cd /mnt

#sudo kill -SIGINT `ps -ef | grep ${PLAY_APP_DIST_NAME} | grep -v grep | awk '{print $2}'`

sudo pgrep -f ${PLAY_APP_NAME_LOWER}

sudo pkill -f ${PLAY_APP_NAME_LOWER}
#sudo kill $(cat ${PLAY_APP_DIST_NAME}/RUNNING_PID)

sleep 20

sudo rm -rf ./${PLAY_APP_DIST_NAME_LOWER}

unzip ${PLAY_APP_DIST_NAME}.zip

cd ${PLAY_APP_DIST_NAME_LOWER}

chmod u+x ./bin/${PLAY_APP_NAME_LOWER}

touch /mnt/${PLAY_APP_NAME_LOWER}_nohup.out

#sudo nohup bin/yosemite-explorer-backend -Dconfig.resource=application.dev.conf -Dlogger.resource=logback.dev.xml -Dhttp.port=80 -J-Xmx1024m -J-Xms1024m -J-server > /mnt/yosemite-explorer-backend_nohup.out 2>&1&
sudo nohup bin/${PLAY_APP_NAME_LOWER} -Dconfig.resource=${PLAY_APP_CONFIG_FILE} -Dlogger.resource=${PLAY_APP_LOGGER_FILE} -Dhttp.port=80 -J-Xmx1024m -J-Xms1024m -J-server > /mnt/${PLAY_APP_NAME_LOWER}_nohup.out 2>&1&

echo server side script finished


