FROM jenkins/jenkins:lts

USER root

# Dependencias base
RUN apt-get update && apt-get install -y wget unzip && rm -rf /var/lib/apt/lists/*

# Variables del Android SDK
ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

# Descargar e instalar Android cmdline-tools
RUN mkdir -p $ANDROID_HOME/cmdline-tools && \
    cd $ANDROID_HOME/cmdline-tools && \
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip && \
    unzip -q commandlinetools-linux-11076708_latest.zip && \
    mv cmdline-tools latest && \
    rm commandlinetools-linux-11076708_latest.zip

# Aceptar licencias e instalar paquetes necesarios
RUN yes | sdkmanager --licenses > /dev/null && \
    sdkmanager "platform-tools" \
               "platforms;android-34" "build-tools;34.0.0" \
               "platforms;android-36" "build-tools;36.0.0"

# Ajustar permisos para que el usuario jenkins pueda usar el SDK
RUN chown -R jenkins:jenkins $ANDROID_HOME

USER jenkins