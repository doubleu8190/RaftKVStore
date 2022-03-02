FROM openjdk:11

RUN mkdir -p /home/admin/RaftKVStore & cp target/*-jar-with-dependencies.jar /home/admin/RaftKVStore/app.jar

WORKDIR /home/admin/RaftKVStore

CMD ["java","jar","app.jar","m","singletong"]
