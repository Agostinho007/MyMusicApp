package com.dev.mymusicapp.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;

/**
 * A anotação @Entity diz ao Room que esta classe representa uma tabela na base de dados.
 * Esta tabela é especial, chama-se "tabela de junção" ou "cross-reference table".
 * A sua única finalidade é ligar as tabelas 'playlists' e 'songs'.
 *
 * primaryKeys Define uma "chave primária composta". Isto significa que a combinação
 * de 'playlistId' e 'dataPath' deve ser única. Na prática, isto impede que a mesma
 * música seja adicionada à mesma playlist mais do que uma vez.
 */
@Entity(primaryKeys = {"playlistId", "dataPath"})
public class PlaylistSongCrossRef {
    /**
     * Este campo armazena o ID da playlist.
     * Funciona como uma "chave estrangeira" que se refere à chave primária (playlistId) da tabela 'playlists'.
     */
    public int playlistId;

    /**
     * Este campo armazena o caminho do ficheiro da música (o ID da música).
     * Funciona como uma "chave estrangeira" que se refere à chave primária (dataPath) da tabela 'songs'.
     * A anotação @NonNull garante que este campo nunca pode ser nulo.
     */
    @NonNull
    public String dataPath;
}