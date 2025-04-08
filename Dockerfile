FROM jenkins/jenkins:lts-jdk17
USER root
RUN apt-get update && apt-get install -y lsb-release

RUN curl -fsSLo /usr/share/keyrings/docker-archive-keyring.asc \
    https://download.docker.com/linux/debian/gpg
RUN echo "deb [arch=$(dpkg --print-architecture) \
    signed-by=/usr/share/keyrings/docker-archive-keyring.asc] \
    https://download.docker.com/linux/debian \
    $(lsb_release -cs) stable" > /etc/apt/sources.list.d/docker.list
RUN apt-get update && apt-get install -y docker-ce-cli

# Create the docker group (if it doesn't exist) and add the Jenkins user to it.
# Adjust the group id (e.g., 999) if your host's docker group uses a different GID.
RUN groupadd -g 999 docker || true && usermod -aG docker jenkins


USER jenkins
RUN jenkins-plugin-cli --plugins "blueocean docker-workflow"