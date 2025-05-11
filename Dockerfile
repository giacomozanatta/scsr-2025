FROM debian:bookworm

RUN apt update && apt install -y git curl build-essential libssl-dev

# rbenv is a version manager tool for the Ruby programming language on Unix-like systems.
# It is useful for switching between Ruby versions and for ensuring that each project runs on the correct Ruby version.
RUN curl -fsSL https://github.com/rbenv/rbenv-installer/raw/HEAD/bin/rbenv-installer | bash
ENV PATH="$PATH:/root/.rbenv/libexec/"
RUN rbenv install 3.1.4 && rbenv global 3.1.4

# ....
WORKDIR /lisa
COPY outputs outputs
COPY analyzers analyzers
COPY analyzers.rb .
COPY main.rb .
