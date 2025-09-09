FROM keynmol/sn-vcpkg:latest as dev

WORKDIR /workdir

# Install NGINX Unit
RUN apt-get update && \
    apt-get install -y curl && \
    curl --output /usr/share/keyrings/nginx-keyring.gpg \
      https://unit.nginx.org/keys/nginx-keyring.gpg && \
    echo 'deb [signed-by=/usr/share/keyrings/nginx-keyring.gpg] https://packages.nginx.org/unit/ubuntu/ jammy unit \
          deb-src [signed-by=/usr/share/keyrings/nginx-keyring.gpg] https://packages.nginx.org/unit/ubuntu/ jammy unit' >> /etc/apt/sources.list.d/unit.list && \
    apt-get update && \
    apt-get install -y unit unit-dev bison flex
RUN if [ "$(uname -m)" = "x86_64" ]; then \
    curl -Lo node-install.tar.xz https://nodejs.org/dist/v22.18.0/node-v22.18.0-linux-x64.tar.xz; \
    else \
    curl -Lo node-install.tar.xz https://nodejs.org/dist/v22.18.0/node-v22.18.0-linux-arm64.tar.xz; \
    fi && \
    tar -xf node-install.tar.xz && rm *.tar.xz && mv node-v22* node-install
ENV PATH /workdir/node-install/bin:$PATH

# vcpkg stuff
RUN sn-vcpkg bootstrap
COPY vcpkg.json .
ENV VCPKG_FORCE_SYSTEM_BINARIES=1
RUN sn-vcpkg install --manifest vcpkg.json

# bootstrap SBT as much as possible
COPY project/build.properties project/build.properties
COPY project/plugins.sbt project/plugins.sbt
RUN sbt --sbt-create version update

COPY . .

# build server binary
RUN sbt buildServerRelease
RUN mkdir empty_dir
RUN cat /etc/passwd | grep unit > passwd
RUN cat /etc/group | grep unit > group
RUN chown unit:unit out/release/server/scala-boot-server
RUN chmod 0777 out/release/server/scala-boot-server

RUN sbt buildWebappRelease

FROM scratch

WORKDIR /workdir

COPY --from=dev /workdir/out/release/server/statedir /workdir/statedir
COPY --from=dev /workdir/out/release/server/scala-boot-server /workdir/scala-boot-server
COPY --from=dev /workdir/out/release/server/static /workdir/static

# unitd dependencies
COPY --from=dev /usr/sbin/unitd /usr/sbin/unitd
COPY --from=dev /workdir/passwd /etc/passwd
COPY --from=dev /workdir/group /etc/group
COPY --from=dev /workdir/empty_dir /var/run

## x86_64 specific files
COPY --from=dev */lib/x86_64-linux-gnu/libm.so.6 /lib/x86_64-linux-gnu/libm.so.6
COPY --from=dev */lib/x86_64-linux-gnu/libpcre2-8.so.0 /lib/x86_64-linux-gnu/libpcre2-8.so.0
COPY --from=dev */lib/x86_64-linux-gnu/libcrypto.so.3 /lib/x86_64-linux-gnu/libcrypto.so.3
COPY --from=dev */lib/x86_64-linux-gnu/libssl.so.3 /lib/x86_64-linux-gnu/libssl.so.3
COPY --from=dev */lib/x86_64-linux-gnu/libc.so.6 /lib/x86_64-linux-gnu/libc.so.6
COPY --from=dev */lib64/ld-linux-x86-64.so.2 /lib64/ld-linux-x86-64.so.2

## aarch64 speicific files
COPY --from=dev */lib/aarch64-linux-gnu/libm.so.6 /lib/aarch64-linux-gnu/libm.so.6
COPY --from=dev */lib/aarch64-linux-gnu/libpcre2-8.so.0 /lib/aarch64-linux-gnu/libpcre2-8.so.0
COPY --from=dev */lib/aarch64-linux-gnu/libcrypto.so.3 /lib/aarch64-linux-gnu/libcrypto.so.3
COPY --from=dev */lib/aarch64-linux-gnu/libssl.so.3 /lib/aarch64-linux-gnu/libssl.so.3
COPY --from=dev */lib/aarch64-linux-gnu/libc.so.6 /lib/aarch64-linux-gnu/libc.so.6
COPY --from=dev */lib/ld-linux-aarch64.so.1 /lib/ld-linux-aarch64.so.1

# scala native dependencies

## x86_64 specific files
COPY --from=dev */lib/x86_64-linux-gnu/libstdc++.so.6 /lib/x86_64-linux-gnu/libstdc++.so.6
COPY --from=dev */lib/x86_64-linux-gnu/libgcc_s.so.1 /lib/x86_64-linux-gnu/libgcc_s.so.1

## aarch64 speicific files
COPY --from=dev */lib/aarch64-linux-gnu/libstdc++.so.6 /lib/aarch64-linux-gnu/libstdc++.so.6
COPY --from=dev */lib/aarch64-linux-gnu/libgcc_s.so.1 /lib/aarch64-linux-gnu/libgcc_s.so.1

ENTRYPOINT [ "unitd", "--statedir", "statedir", "--log", "/dev/stdout", "--no-daemon" ]
