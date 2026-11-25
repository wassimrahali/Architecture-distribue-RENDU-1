const express = require('express');
const Eureka = require('eureka-js-client').Eureka;

const PORT = parseInt(process.env.PORT || '8083', 10);
const APP_NAME = 'MEETING';
const HOST = process.env.EUREKA_INSTANCE_HOST || 'localhost';
const EUREKA_HOST = process.env.EUREKA_HOST || 'localhost';
const EUREKA_PORT = parseInt(process.env.EUREKA_PORT || '8761', 10);

const app = express();
app.use(express.json());

// ---- mini CRUD en mémoire ----
let meetings = [];
let nextId = 1;

app.get('/meetings', (req, res) => res.json(meetings));
app.get('/meetings/:id', (req, res) => {
  const m = meetings.find(x => x.id === +req.params.id);
  m ? res.json(m) : res.status(404).json({ error: 'not found' });
});
app.post('/meetings', (req, res) => {
  const m = { id: nextId++, ...req.body };
  meetings.push(m);
  res.status(201).json(m);
});
app.put('/meetings/:id', (req, res) => {
  const i = meetings.findIndex(x => x.id === +req.params.id);
  if (i < 0) return res.status(404).json({ error: 'not found' });
  meetings[i] = { ...req.body, id: +req.params.id };
  res.json(meetings[i]);
});
app.delete('/meetings/:id', (req, res) => {
  meetings = meetings.filter(x => x.id !== +req.params.id);
  res.status(204).end();
});

// ---- health (utilisé par Eureka) ----
app.get('/health', (req, res) => res.json({ status: 'UP' }));

// ---- découverte : voir les instances d'un autre service ----
app.get('/discover/:app', (req, res) => {
  const instances = client.getInstancesByAppId(req.params.app.toUpperCase());
  res.json(instances.map(i => ({ app: i.app, host: i.hostName, ip: i.ipAddr, port: i.port && i.port.$ })));
});

// ---- enregistrement Eureka ----
const client = new Eureka({
  instance: {
    app: APP_NAME,
    instanceId: `${APP_NAME.toLowerCase()}:${HOST}:${PORT}`,
    hostName: HOST,
    ipAddr: HOST,
    statusPageUrl: `http://${HOST}:${PORT}/health`,
    healthCheckUrl: `http://${HOST}:${PORT}/health`,
    homePageUrl: `http://${HOST}:${PORT}/meetings`,
    port: { '$': PORT, '@enabled': true },
    vipAddress: APP_NAME.toLowerCase(),
    dataCenterInfo: { '@class': 'com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo', name: 'MyOwn' },
    registerWithEureka: true,
    fetchRegistry: true,
  },
  eureka: { host: EUREKA_HOST, port: EUREKA_PORT, servicePath: '/eureka/apps/', maxRetries: 30, requestRetryDelay: 5000 },
});

app.listen(PORT, () => {
  console.log(`MEETING service on :${PORT}`);
  client.start(err => console.log(err ? `Eureka error: ${err}` : 'Registered in Eureka'));
});

const bye = () => client.stop(() => process.exit());
process.on('SIGINT', bye);
process.on('SIGTERM', bye);
