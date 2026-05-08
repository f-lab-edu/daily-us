# Kafka 실행
- [Apache Kafka Quick Start](https://kafka.apache.org/quickstart/?utm_source=chatgpt.com)
- [Zookeeper에 의존하지 않는 Kafka](https://aws.amazon.com/ko/blogs/tech/amazon-msk-kraft-mode/)

## 1. 이미지 Pull
```bash
docker pull apache/kafka:4.2.0
```

## 2. 컨테이너 실행
```bash
docker run -d -p 9092:9092 --name kafka apache/kafka:4.2.0
```

## 3. 토픽 생성
```bash
docker exec -it kafka /opt/kafka/bin/kafka-topics.sh --create --topic post.created --bootstrap-server localhost:9092
```