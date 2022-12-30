const pool = require("./db")

const getAllProducts = (req, res) => {
    pool.query('SELECT * FROM products;').then(results => {
        res.status(200).json(results.rows);
    }).catch(error => {
        res.status(500).json({result: "Ocorreu um erro ao tentar obter os produtos"});
        console.log(error);
    });

}

const getProductByID = (req, res) => {
    let id = req.params.id;

    pool.query(`SELECT * FROM products WHERE id=${id};`).then(results => {
        res.status(200).json(results.rows[0]);
    }).catch(error => {
        res.status(500).json({result: "Ocorreu um erro ao tentar obter o produto"});
        console.log(error);
    });
}

const createProduct = (req, res) => {
    let body = req.body;

    if (body) {
        pool.query(`INSERT INTO "products"("icon", "name", "quantity", "price") 
                    VALUES('${body.icon}', '${body.name}', '${body.quantity}', '${body.price}');
                    SELECT currval('products_id_seq');`).then(results => {
            res.status(200).json(results[1].rows[0].currval);
            //console.log("Produto Adicionado");
        }).catch(error => {
            res.status(500).json("Ocorreu um erro ao tentar criar o produto");
            console.log(error);
        });
    } else {
        res.status(400).json("Não existem dados para fazer a criação de um novo produto");
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
        pool.query(`UPDATE products SET icon = '${body.icon}', name = '${body.name}', quantity = '${body.quantity}', price = '${body.price}' WHERE id='${id}'`).then(results => {
            res.status(200).json("Produto Atualizado");
        }).catch(error => {
            res.status(500).json("Ocorreu um erro ao tentar atualizar o produto");
            console.log(error);
        });
    } else {
        res.status(400).json("Não existem dados para fazer a atualização do produto");
    }
}


module.exports = {
    getAllProducts,
    getProductByID,
    createProduct,
    deleteProductByID,
    updateProductByID,
}
