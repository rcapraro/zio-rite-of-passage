ZIO Rite of passage
===================

# Run PostgreSQL database

```shell
docker compose up -d
```

# Install npm dependencies

in the `app` folder

```shell
npm install
```

# Serve frontend with Parcel

in the `app` folder

```shell
npm run start
```

# Compile Laminar js (development mode)

```shell
sbt
project app
~fastOptJS
```