# Backend

## Cobertura JaCoCo

La cobertura se calcula en `verify`, no en `test`.

```bash
cd backend
./mvnw verify
```

El build falla si la cobertura de líneas baja del 70%.

Para ver el informe HTML local:

```bash
open target/site/jacoco/index.html
```

Si el contexto completo de Spring arranca contra Postgres local, levanta antes el contenedor del proyecto desde la raíz:

```bash
docker compose up -d
```
