package com.dev.mymusicapp.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * A anotação @Entity diz ao Room que esta classe representa uma tabela na base de dados.
 * O nome da tabela será "playlists".
 */
@Entity(tableName = "playlists")
public class Playlist {
    /**
     * @PrimaryKey define que este campo é a chave primária da tabela.
     * 'autoGenerate = true' instrui o Room a gerar automaticamente um ID numérico único
     * e incremental para cada nova playlist que for inserida no banco de dados.
     */
    @PrimaryKey(autoGenerate = true)
    public int playlistId; // O identificador único para cada playlist.

    /**
     * O nome da playlist, definido pelo utilizador (ex: "Músicas para Correr", "Favoritas").
     * Como os campos são públicos, o Room pode acedê-los diretamente para ler e escrever
     * no banco de dados, simplificando o código da entidade.
     */
    public String name;
}