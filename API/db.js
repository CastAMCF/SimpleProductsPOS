const settings = require('./config.json');
const Pool = require('pg').Pool;

const pool = new Pool({
    user: settings.PostgreSQL.user,
    host: settings.PostgreSQL.host,
    database: settings.PostgreSQL.database,
    password: settings.PostgreSQL.password,
    port: settings.PostgreSQL.port,
    ssl: { rejectUnauthorized: false },
})



module.exports = pool;