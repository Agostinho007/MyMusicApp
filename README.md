# My Music App - Leitor de Música para Android

## 📖 Descrição do Projeto

**My Music App** é uma aplicação de leitor de música para Android, desenvolvida em Java, que oferece uma experiência completa para ouvir músicas locais. A aplicação permite aos utilizadores explorar as suas bibliotecas de música, criar e gerir playlists, e até identificar músicas que estão a tocar no ambiente.

Este projeto foi desenvolvido seguindo as melhores práticas de desenvolvimento Android, incluindo a arquitetura **MVP (Model-View-Presenter)** e a utilização de componentes modernos para garantir uma aplicação robusta, manutenível e com uma excelente experiência de utilizador.

## ✨ Funcionalidades Principais

* **Leitor de Música Completo:** Reproduz, pausa, avança, retrocede, com modos **Shuffle** e **Repeat** (repetir uma/todas).
* **Reprodução em Background:** A música continua a tocar mesmo quando a aplicação está minimizada ou o ecrã está desligado, graças a um **Foreground Service**.
* **Controlo por Notificação:** Controlo total da reprodução através de uma notificação persistente.
* **Gestão de Playlists:**
    * Criação de múltiplas playlists.
    * Adição de músicas às playlists de forma individual ou em massa.
    * Remoção de músicas de uma playlist (com clique longo).
    * Remoção de playlists inteiras (com clique longo).
* **Busca Inteligente:** Pesquisa em tempo real por título ou artista na lista de músicas.
* **Identificação de Músicas:** Utiliza a API da **ACRCloud** para "ouvir" e identificar músicas que estejam a tocar no ambiente.
* **Interface Moderna e Polida:**
    * Menus de acesso rápido com **Multi-FloatingActionButton**.
    * Carregamento de **capas de álbuns** reais.
    * Animações suaves, incluindo a rotação da capa do álbum e transições entre telas.
    * Diálogos e alertas estilizados com o **Material Design**.


## 🛠️ Tecnologias e Bibliotecas Utilizadas

* **Linguagem:** Java
* **Arquitetura:** MVP (Model-View-Presenter)
* **Componentes Principais:**
    * **AndroidX Libraries:** (AppCompat, RecyclerView, ConstraintLayout) para a base da UI e compatibilidade.
    * **Material Design Components:** Para criar uma interface moderna e responsiva (FABs, Diálogos, Estilos).
    * **ExoPlayer:** A biblioteca recomendada pela Google para reprodução de áudio e vídeo, mais poderosa e flexível que o `MediaPlayer`.
    * **Room Persistence Library:** Para a gestão do banco de dados SQLite de forma simples e segura, usada no sistema de playlists.
    * **Glide:** Para o carregamento eficiente e manipulação de imagens (capas dos álbuns).
    * **ACRCloud SDK:** SDK externo para a funcionalidade de reconhecimento de áudio.

## ⚙️ Como Configurar e Executar o Projeto

1.  **Clonar o Repositório:**
    ```bash
    git clone https://github.com/Agostinho007/MyMusicApp.git
    ```
2.  **Abrir no Android Studio:**
    * Abra o Android Studio e selecione "Open an existing project".
    * Navegue até à pasta do projeto clonado e selecione-a.
3.  **Configurar a API da ACRCloud:**
    * Crie uma conta gratuita em [ACRCloud](https://www.acrcloud.com/).
    * Crie um projeto do tipo "Audio & Video Recognition" e obtenha as suas credenciais.
    * No ficheiro `MainActivity.java` e `PlaylistsActivity.java`, encontre o método `setupAcrCloud()` e substitua os placeholders pelas suas credenciais:
        ```
        acrCloudConfig.host = "SUA_HOST_AQUI";
        acrCloudConfig.accessKey = "SUA_ACCESS_KEY_AQUI";
        acrCloudConfig.accessSecret = "SUA_ACCESS_SECRET_AQUI";
        ```
4.  **Sincronizar e Executar:**
    * O Android Studio deverá sincronizar o projeto automaticamente. Se não, clique no ícone de "Sync Project with Gradle Files".
    * Clique em "Run 'app'" para instalar a aplicação num emulador ou dispositivo físico.

---
