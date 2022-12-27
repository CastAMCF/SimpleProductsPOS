const pool = require("./db")

const getAllProducts = (req, res) => {
    pool.query('SELECT * FROM products;').then(results => {
        res.status(200).json(results.rows);
    }).catch(error => {
        console.log(error);
    });

}

const getProductByName = (req, res) => {
    let name = req.query.nome;
    let condition = ` WHERE ("name" ILIKE '%${name}%'`;

    pool.query(`SELECT * FROM products${condition};`).then(results => {
        res.status(200).json(results.rows);
    }).catch(error => {
        console.log(error);
    });
}

const getProductByID = (req, res) => {
    let id = req.params.id;

    pool.query(`SELECT * FROM products WHERE id=${id};`).then(results => {
        res.status(200).json(results.rows);
    }).catch(error => {
        console.log(error);
    });
}

const createProduct = (req, res) => {
    let body = req.body;
    
    if (body) {
        pool.query(`INSERT INTO "products"("iconid", "name", "quantity", "price") 
                    VALUES('${body.iconid}', '${body.name}', '${body.quantity}', '${body.price}')`).then(results => {
            res.status(200).json("Produto Adicionado");
        }).catch(error => {
            res.status(500).json("Ocorreu um erro ao tentar criar o produto");
            console.log(error);
        });
    } else {
        res.status(400).json({result: "Não existem dados para fazer a criação de um novo produto"});
    }
}

const deleteProductByID = (req, res) => {
    let id = req.params.id;

    pool.query(`DELETE FROM products WHERE id=${id};`).then(results => {
        res.status(200).json("Produto eliminado");
    }).catch(error => {
        res.status(500).json("Ocorreu um erro ao tentar eliminar o produto");
        console.log(error);
    });
}

const updateProductByID = (req, res) => {
    let body = req.body;
    let id = req.params.id
    
    if (body) {
        pool.query(`UPDATE products SET iconid = '${body.iconid}', name = '${body.name}', quantity = '${body.quantity}', price = '${body.price}' WHERE id='${id}'`).then(results => {
            res.status(200).json("Produto Atualizado");
        }).catch(error => {
            res.status(500).json("Ocorreu um erro ao tentar atualizar o produto");
            console.log(error);
        });
    } else {
        res.status(400).json({result: "Não existem dados para fazer a atualização do produto"});
    }
}


module.exports = {
    getAllProducts,
    getProductByName,
    getProductByID,
    createProduct,
    deleteProductByID,
    updateProductByID,
}

/** 
 * fetch("http://127.0.0.1:4000/products", {
        method: 'POST',
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
            iconid: 5,
            name: "tremoços",
            quantity: 45,
            price: 1.99
        })
}).then(res => res.json())
.then(console.log) 
*/