# yosemite-explorer-backend
YOSEMITE Blockchain Explorer API Server Backend connected to a mongodb blockchain log store managed by mongo_db_plugin of YOSEMITE blockchain.

### YOSEMITE Blockchain Explorer API server (Public-Test-Net)
http://testnet-explorer-api.yosemitelabs.org

### REST API Testing and Documentation (Swagger)
http://testnet-explorer-api.yosemitelabs.org


### Action APIs

API endpoint | Description
------------ | -------------
GET /action/{id} | Get a action data by action-id
GET /transaction/{txid}/actions/{idx} | Get a action in a Transaction by transaction-id and action index
GET /transaction/{txid}/actions | Search all Actions in a Transaction
GET /account/{name}/actions | Search all Actions received by an account

### Block APIs

API endpoint | Description
------------ | -------------
GET /blocks | Search all Blocks
GET /block/id/{id} | Get a block data by block-id
GET /block/{num} | Get a block data by block-number 


### Transaction APIs

API endpoint | Description
------------ | -------------
GET /block/{blockNum}/transactions | Search all Transactions in a Block
GET /transactions | Search all Transactions
GET /transaction/{id} | Get a transaction data by transaction-id