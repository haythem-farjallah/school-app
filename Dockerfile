FROM jenkins/jenkins:lts-jdk17

USER root

# Install prerequisites (lsb-release and curl)
RUN apt-get update && apt-get install -y lsb-release curl

# Add Docker’s official GPG key and repository, then install docker-ce-cli
RUN curl -fsSLo /usr/share/keyrings/docker-archive-keyring.asc \
      https://download.docker.com/linux/debian/gpg && \
    echo "deb [arch=$(dpkg --print-architecture) \
    signed-by=/usr/share/keyrings/docker-archive-keyring.asc] \
    https://download.docker.com/linux/debian \
    $(lsb_release -cs) stable" > /etc/apt/sources.list.d/docker.list && \
    apt-get update && apt-get install -y docker-ce-cli

# Install docker-compose (Docker Compose V2)
RUN curl -L "https://github.com/docker/compose/releases/download/v2.20.2/docker-compose-$(uname -s)-$(uname -m)" \
      -o /usr/local/bin/docker-compose && \
    chmod +x /usr/local/bin/docker-compose

# Create the docker group (if it doesn't exist) and add the Jenkins user to it
RUN groupadd -g 999 docker || true && usermod -aG docker jenkins

USER jenkins

# Install Jenkins plugins; blueocean for UI enhancements and docker-workflow for Docker pipeline support.
RUN jenkins-plugin-cli --plugins "blueocean docker-workflow"

# Optional: Display versions of docker and docker-compose for debugging.
RUN docker --version && docker-compose --version