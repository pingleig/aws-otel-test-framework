validate:
	awsl isengard get > creds.env
	docker-compose -f validator_docker_compose.yml up

build:
	docker-compose -f validator_docker_compose.yml build
