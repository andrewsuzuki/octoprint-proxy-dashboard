# Makefile
# ! for deploy tasks !

name_api := api
name_cam := cam
name_front_end := front-end

.PHONY: deploy-api deploy-api-network-host deploy-cam deploy-front-end

# supply p, the host port (e.g. 8080)
# supply c, the host absolute path to config file
# supply o, the allowed origins (e.g. "*")
deploy-api:
	test -n "$(p)" || exit 1; \
	test -n "$(c)" || exit 1; \
	test -n "$(o)" || exit 1; \
	docker stop $(name_api); \
	docker rm $(name_api); \
	docker build -t $(name_api) ./api && \
	echo "BUILD SUCCEEDED, RUNNING..." && \
	docker run --name $(name_api) \
			   -t \
			   --restart=unless-stopped \
			   -p $(p):$(p) \
			   -v $(shell dirname $(c)):/configs \
			   $(name_api) --port "$(p)" --origins "$(o)" --config "/configs/$(shell basename $(c))" || \
	docker rm -f $(name_api) # if run failed

# special deploy-api with network=host instead of -p
# use if there are cam services on host
# supply p, the host port (e.g. 8080)
# supply c, the host absolute path to config file
# supply o, the allowed origins (e.g. "*")
deploy-api-network-host:
	test -n "$(p)" || exit 1; \
	test -n "$(c)" || exit 1; \
	test -n "$(o)" || exit 1; \
	docker stop $(name_api); \
	docker rm $(name_api); \
	docker build -t $(name_api) ./api && \
	echo "BUILD SUCCEEDED, RUNNING..." && \
	docker run --name $(name_api) \
			   -t \
			   --restart=unless-stopped \
			   --network host \
			   -v $(shell dirname $(c)):/configs \
			   $(name_api) --port "$(p)" --origins "$(o)" --config "/configs/$(shell basename $(c))" || \
	docker rm -f $(name_api) # if run failed

# supply p, the host port (e.g. 8020), and d, the host video device (e.g. /dev/video0)
deploy-cam:
	test -n "$(p)" || exit 1; \
	test -n "$(d)" || exit 1; \
	docker stop $(name_cam); \
	docker rm $(name_cam); \
	docker build -t $(name_cam) ./cam && \
	echo "BUILD SUCCEEDED, RUNNING..." && \
	docker run --name $(name_cam) \
			   -t \
			   --restart=unless-stopped \
			   --device $(d):$(d) \
			   -p $(p):8020 \
			   $(name_cam) --device $(d) || \
	docker rm -f $(name_cam) # if run failed

# supply p, the host port (e.g. 80), and api_base_url (e.g. http://192.168.1.10:8080)
deploy-front-end:
	test -n "$(api_base_url)" || exit 1; \
	test -n "$(p)" || exit 1; \
	docker stop $(name_front_end); \
	docker rm $(name_front_end); \
	docker build --build-arg api_base_url=$(api_base_url) -t $(name_front_end) ./front-end && \
	echo "BUILD SUCCEEDED, RUNNING..." && \
	docker run --name $(name_front_end) \
			   -t \
			   --restart=unless-stopped \
			   -p $(p):80 \
			   $(name_front_end) || \
	docker rm -f $(name_front_end) # if run failed

