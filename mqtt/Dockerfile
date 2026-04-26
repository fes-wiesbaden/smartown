FROM eclipse-mosquitto:2.0

USER root

COPY config/mosquitto.conf /mosquitto/config/mosquitto.conf
COPY docker-entrypoint.sh /usr/local/bin/smartown-mosquitto-entrypoint.sh

RUN chmod +x /usr/local/bin/smartown-mosquitto-entrypoint.sh \
    && sed -i 's/\r$//' /usr/local/bin/smartown-mosquitto-entrypoint.sh \
    && mkdir -p /mosquitto/data /mosquitto/log \
    && chown -R mosquitto:mosquitto /mosquitto

USER mosquitto

EXPOSE 1883

ENTRYPOINT ["/usr/local/bin/smartown-mosquitto-entrypoint.sh"]
