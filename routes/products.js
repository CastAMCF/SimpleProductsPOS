const express = require('express');
const router = express.Router();

const controller = require('../controllers/productsController');

router.get("/", controller.getAllProducts);

router.get("/:id", controller.getProductByID);

router.post("/", controller.createProduct);

router.put("/:id", controller.updateProductByID);

router.delete("/:id", controller.deleteProductByID);

module.exports = router;