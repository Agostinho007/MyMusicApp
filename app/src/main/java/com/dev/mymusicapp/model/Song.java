package com.dev.mymusicapp.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

/**
 * A anotação @Entity diz ao Room que esta classe representa uma tabela na base de dados.
 * O nome da tabela será "songs".
 */
@Entity(tableName = "songs")
/**
 * A classe Song é um "Model" ou POJO (Plain Old Java Object) que representa uma única música.
 * Ela implementa 'Serializable' para que objetos desta classe possam ser passados entre Activities
 * através de Intents (por exemplo, ao passar uma lista de músicas para a PlayerActivity).
 */
public class Song implements Serializable {

    /**
     * @PrimaryKey define que este campo é a chave primária da nossa tabela no banco de dados.
     * Cada música terá um caminho de ficheiro único, o que o torna um bom candidato para chave primária.
     * @NonNull garante que este campo nunca pode ser nulo.
     */
    @PrimaryKey
    @NonNull
    private final String dataPath; // O caminho completo do ficheiro de áudio no armazenamento do dispositivo.

    // Outros campos que armazenam as propriedades da música.
    private final long id; // O ID único da música no MediaStore do Android.
    private final String title; // O título da música.
    private final String artist; // O nome do artista.
    private final long duration; // A duração da música em milissegundos.
    private final String albumArtUri; // O URI (endereço) para a imagem da capa do álbum.

    /**
     * Construtor da classe Song. É usado para criar uma nova instância de uma música
     * com todos os seus atributos.
     */
    public Song(long id, @NonNull String title, @NonNull String artist, @NonNull String dataPath, long duration, String albumArtUri) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.dataPath = dataPath;
        this.duration = duration;
        this.albumArtUri = albumArtUri;
    }

    // --- MÉTODOS GETTER ---
    // Os métodos abaixo são "getters" públicos que permitem que outras classes
    // leiam os valores dos campos privados desta classe, seguindo o princípio de encapsulamento.

    public long getId() {
        return id;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    @NonNull
    public String getArtist() {
        return artist;
    }

    @NonNull
    public String getDataPath() {
        return dataPath;
    }

    public long getDuration() {
        return duration;
    }

    public String getAlbumArtUri() {
        return albumArtUri;
    }
}