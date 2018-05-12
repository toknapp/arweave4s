dev-chain: build-image
	docker run --rm --name arweave-dev-chain --publish 1984:1984 arweave \
		no_auto_join init mine peer 127.0.0.1:9

testnet: build-image
	cat peers.testnet | sed 's/^/peer /' \
		| xargs docker run --name=arweave-testnet arweave

build-image:
	docker build -t arweave .

.PHONY: build-image testnet dev-chain
