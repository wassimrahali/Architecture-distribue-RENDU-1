# TP Eureka – Applications Web Distribuées (UP_WEB 2026-2027)

| Microservice | Techno | Port | Nom Eureka | Dossier |
|---|---|---|---|---|
| Eureka Server | Spring Boot | 8761 | – | `eureka-server/` |
| Candidat | Spring Boot + H2 | 8081 (+8085) | CANDIDAT | `candidat-service/` |
| Job | Spring Boot + MySQL | 8082 | JOB | `job-service/` |
| Meeting | Node.js / Express | 8083 | MEETING | `meeting-service/` |
| Notification | Python / FastAPI | 8084 | NOTIFICATION | `notification-service/` |

## Lancement rapide (Docker)
```bash
docker compose up --build
```
Ouvrir **http://localhost:8761** : les 5 applications (CANDIDAT avec 2 instances) apparaissent après ~30-60 s.

Arrêt : `docker compose down` (ajouter `-v` pour supprimer les données MySQL).

## Checklist des tâches
- [x] Partie 1 – Serveur Eureka (`@EnableEurekaServer`, `register-with-eureka=false`, `fetch-registry=false`, port 8761)
- [x] Partie 2 – Candidat & Job clients Eureka (`@EnableDiscoveryClient`, `defaultZone`, noms `candidat` / `job`)
- [x] Partie 2 – Étape 4 : 2e instance de Candidat (service `candidat-2`, port 8085)
- [x] Partie 3 – Meeting (Node.js, `eureka-js-client`) et Notification (Python, `py-eureka-client`)
- [x] Bonus : Job appelle Candidat **par son nom** (`http://candidat/...`, `@LoadBalanced`)

## Lancement sans Docker (Partie 2, étape 4)
Prérequis : Java 17, Maven, MySQL (pour Job), Node 20, Python 3.12.
```bash
# 1. Eureka
cd eureka-server && mvn spring-boot:run
# 2. Candidat (instance 1) puis instance 2 sur un autre port
cd candidat-service && mvn spring-boot:run
cd candidat-service && mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8085
# 3. Job
cd job-service && mvn spring-boot:run
# 4. Meeting
cd meeting-service && npm install && npm start
# 5. Notification
cd notification-service && pip install -r requirements.txt && uvicorn main:app --port 8084
```

## Tests
```bash
curl -X POST localhost:8081/candidats -H "Content-Type: application/json" \
  -d '{"nom":"Ben Ali","prenom":"Sara","email":"sara@mail.com","poste":"Dev Java"}'
curl localhost:8081/candidats

curl -X POST localhost:8082/jobs -H "Content-Type: application/json" \
  -d '{"titre":"Dev Backend","entreprise":"Esprit","localisation":"Tunis","description":"Spring Boot"}'
curl localhost:8082/jobs

curl -X POST localhost:8083/meetings -H "Content-Type: application/json" -d '{"titre":"Entretien","date":"2026-12-01"}'
curl -X POST localhost:8084/notifications -H "Content-Type: application/json" -d '{"destinataire":"sara@mail.com","message":"Entretien confirmé"}'

# Découverte de services : Job appelle "candidat" par son nom (répétez : alternance 8081 / 8085)
for i in 1 2 3 4; do curl -s localhost:8082/jobs/candidats-instance; echo; done

# Voir les instances depuis Node et Python
curl localhost:8083/discover/candidat
curl localhost:8084/discover/candidat

# API REST d'Eureka
curl -H "Accept: application/json" localhost:8761/eureka/apps
```

## Étape 3 (facultative) : authentification Eureka
Ajouter `spring-boot-starter-security` à `eureka-server`, une `SecurityFilterChain` avec `csrf.disable()` + `httpBasic`, puis
`eureka.client.service-url.defaultZone=http://user:password@eureka:8761/eureka` dans les clients Spring.

## Notes
- Heartbeat toutes les 30 s ; instance retirée du registre après 90 s sans heartbeat.
- Dans Docker, `eureka.instance.prefer-ip-address=true` permet aux services de s'atteindre via l'IP du conteneur.
- H2 console : http://localhost:8081/h2-console (JDBC URL `jdbc:h2:mem:candidatdb`, user `sa`).
