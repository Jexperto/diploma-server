FROM openjdk:17-jdk-oraclelinux8

RUN mkdir /lol
ADD build/install/* /lol

CMD ["/lol/bin/diploma"]