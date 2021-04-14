validate:
	awsl isengard get > creds.env
	docker-compose -f validator_docker_compose.yaml up

build:
	docker-compose -f validator_docker_compose.yaml build
