IMAGE=arweave-scripts
DOCKER=docker
DOCKER_IMAGE_FLAG=.docker-image-flag

all: $(DOCKER_IMAGE_FLAG)

$(DOCKER_IMAGE_FLAG): $(wildcard *.sc) Dockerfile
	$(DOCKER) build --tag=$(IMAGE) .
	touch $@

clean:
	rm -f $(DOCKER_IMAGE_FLAG)

.PHONY: clean all
