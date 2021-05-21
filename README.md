# Real Time Database aka RTDB

This project uses Quarkus, the Supersonic Subatomic Java Framework.

Real Time Database is a project to notice live database changes.

This project exposes apis to configure a project and its collections. It relies on MongoDB.

The data modifications will be visible in live a la firebase. Clients should subscribe to a websocket to get the
modifications.

## Stack

- Quarkus
- MongoDB
- Kafka
  - Kafka Connect
  - Debezium Connector for CDC (Change Data Capture)
- Websockets

## Features

- Create a project and its collections
- Listen MongoDB events through Kafka Connect and CDC (Change Data Capture) with Debezium

## Dev

You can setup a running environment with Zookeeper, Kafka, Debezium and Mongo using docker-compose.

```shell script
docker-compose up
```

You can run your application in dev mode that enables live coding using:
```shell script
./mvnw compile quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

### Mongo

The MongoDB instance is initialized in a replica set mode. Normally, the docker-compose command does it for you.

### Debezium

Debezium requires a [MongoDB connector](dbz-mongodb-connector.json).

Here are the curl commands to add the MongoDB connector:

```shell script
curl -X DELETE localhost:8083/connectors/rtdb-connector
curl -X POST -H "Accept:application/json" -H "Content-Type:application/json" localhost:8083/connectors/ -d @dbz-mongodb-connector.json
curl -X GET -H "Accept:application/json" -H "Content-Type:application/json" localhost:8083/connectors
```

By default, the reserved databases (admin, config and local) are not monitored.

## Kafka

### Consumers config

See [Consumers configuration](https://kafka.apache.org/documentation/#consumerconfigs)

The `metadata.max.age` property is set to 30 seconds. It means that the quarkus app will receive new items
from any new collections after 30 seconds. The default value was 5 minutes.

TODO: see if there is a way to auto-detect the new topics created.

## API Reference

#### Create a project

```http
  POST /admin/api/projects
```

| Parameter | Type     | Description                |
| :-------- | :------- | :------------------------- |
| `name` | `string` | **Required**. The project name |

#### Create a table

```http
  POST /admin/api/projects/{projectId}/collections
```

| Parameter | Type     | Description                       |
| :-------- | :------- | :-------------------------------- |
| `name`      | `string` | **Required**. The collection name |

## Database structure

#### Projects

Collection name: `rtdb-projects`

| Field         | Type            |
| :--------     | :-------        |
| `_id`         | `ObjectId`      |
| `name`        | `String`        |
| `apikey`      | `String`        |
| `active`      | `Boolean`       |
| `collections` | `List<String>`  |
| `createdAt`   | `Long`          |

A project has a global apikey.

The project name and collections names should respect
the [MongoDB restrictions names](https://docs.mongodb.com/manual/reference/limits/#std-label-restrictions-on-db-names).

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory. Be aware that it’s not an _über-jar_ as
the dependencies are copied into the `target/quarkus-app/lib/` directory.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.type=uber-jar
```

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Pnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Pnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/db-realtime-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/maven-tooling.html
.
