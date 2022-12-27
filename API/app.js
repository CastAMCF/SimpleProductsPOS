const express = require('express');

// express app
const app = express();

// configure express to use json as middle-ware
app.use(express.json());
app.use(function(req, res, next) {
  res.header("Access-Control-Allow-Origin", "*");
  res.header("Access-Control-Allow-Method", "POST, GET, OPTIONS");
  res.header("Access-Control-Allow-Methods", "PUT, DELETE");
  res.header("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");
  res.header("Allow", "*");
  next();
});

// listen for requests
const PORT = process.env.PORT || 4000;
app.listen(PORT, () => console.log(`Server running on port ${PORT}`));

const apiRouter = require("./routes")
app.use('/', apiRouter);