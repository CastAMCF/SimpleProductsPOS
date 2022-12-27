const express = require('express');
const router = express.Router();

const controller = require('./controller');

router.get("/products", controller.getAllProducts);

router.get("/products/:id", controller.getProductByID);

router.post("/products", controller.createProduct);

router.put("/products/:id", controller.updateProductByID);

router.delete("/products/:id", controller.deleteProductByID);

module.exports = router;