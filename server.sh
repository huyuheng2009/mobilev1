#!/bin/sh
# require jre/jdk 1.6+
#
# by hanlei

script_dir=`dirname $0`
APP_HOME=`cd "$script_dir"; pwd`
echo "application home: $APP_HOME"
cd $APP_HOME

PIDFILE=$APP_HOME/.pid
CLASSPATH=$APP_HOME/classes:lib/jpos_ext-1.1.0.jar

JAVA_OPTS="-server -Xmx128M -Xms128M -XX:MaxPermSize=64m -Xss228k -XX:+DisableExplicitGC -XX:+UseParallelGC -XX:ParallelGCThreads=5 -Dfile.encoding=UTF-8"
DEPLOY_DIR="deploy"
ARGS_OPTS="-d $DEPLOY_DIR -r true"

start() {
	rm -f $APP_HOME/$DEPLOY_DIR/shutdown.xml
	java -cp $CLASSPATH $JAVA_OPTS org.jpos.ext.groovy.Boot $ARGS_OPTS > /dev/null 2>&1 &
	echo $!>$PIDFILE
	echo "running pid: $!"
}

stop() {
	echo '<shutdown/>' > $APP_HOME/$DEPLOY_DIR/shutdown.xml
	sleep 3s
	rm -f $PIDFILE
}

net() {
    netstat -anp | grep `cat $PIDFILE`
}

log() {
    tail -fn 300 $APP_HOME/log/mobile.log
}

lsof() {
    /usr/sbin/lsof -p `cat $PIDFILE`
}

heap() {
    dd=`date +%m%d-%H%M`
    jmap -histo `cat $PIDFILE` > $APP_HOME/log/heap/$dd.txt
    jmap -dump:format=b,file=$APP_HOME/log/heap/$dd.bin `cat $PIDFILE`
}

gc() {
	jstat -gc `cat $PIDFILE` 5000
}


case "$1" in
    net)
        net;;
    log)
        log;;
    gc)
        gc;;
    lsof)
        lsof;;
    heap)
        heap;;
    start)
        start;;
    stop)
        stop;;
    restart)
        stop
        start;;
    *)
        echo "Usage: exchange {start|stop|restart|net|log|lsof|heap|gc}"
        exit;
esac
exit 0;
