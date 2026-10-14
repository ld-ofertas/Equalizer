# Equalizer para Android

Aplicativo Android nativo com player de áudio local e equalizador de 5 bandas, controles de reprodução e perfis de som.

## Recursos
- Selecionar arquivos de áudio do dispositivo.
- Reproduzir, pausar e parar a música.
- Ajustar cinco bandas de frequência quando suportadas pelo dispositivo.
- Perfis Normal, Rock, Pop, Jazz, Clássica, Dance e Vocal.
- Interface escura.

## Gerar o APK
1. Abra a aba **Actions** deste repositório no GitHub.
2. Selecione **Build Android APK** e pressione **Run workflow**, ou aguarde o build automático após um push.
3. Abra a execução concluída e baixe o artefato **Equalizer-Android-APK**.
4. Extraia o arquivo `app-debug.apk` e instale-o no Android. Talvez seja necessário autorizar a instalação de apps dessa origem.

## Limitação importante
O equalizador processa o áudio reproduzido dentro deste aplicativo. O Android não permite que um app comum aplique de forma confiável efeitos ao som de todos os outros aplicativos; isso depende do aparelho, da versão do Android e das permissões do sistema.

## Requisitos de compilação
JDK 17, Gradle 8.9 e Android Gradle Plugin 8.7.3. O build é feito automaticamente pelo GitHub Actions.
