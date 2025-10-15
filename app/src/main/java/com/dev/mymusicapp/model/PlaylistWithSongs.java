package com.dev.mymusicapp.model;

import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;

import java.util.List;

/**
 * Esta classe NÃO é uma tabela (@Entity) na base de dados.
 * É uma classe de "relação" ou um DTO (Data Transfer Object). A sua finalidade é permitir que o Room
 * execute uma consulta complexa (um JOIN) e nos devolva um objeto simples que contém uma playlist
 * e a sua lista correspondente de músicas.
 */
public class PlaylistWithSongs {
    /**
     * A anotação @Embedded diz ao Room para tratar os campos do objeto 'Playlist'
     * (como 'playlistId' e 'name') como se fizessem parte desta classe principal.
     * Este é o objeto "pai" da nossa relação.
     */
    @Embedded public Playlist playlist;

    /**
     * A anotação @Relation define como o objeto "pai" (Playlist) se relaciona com
     * a lista de objetos "filho" (List<Song>).
     *
     * @param parentColumn A coluna na tabela do objeto pai ('Playlist') usada para a ligação (neste caso, a sua chave primária 'playlistId').
     * @param entityColumn A coluna na tabela do objeto filho ('Song') usada para a ligação (neste caso, a sua chave primária 'dataPath').
     * @param associateBy  A anotação @Junction especifica a tabela de junção (cross-reference table) que
     * define a relação de muitos-para-muitos. O Room usará a PlaylistSongCrossRef
     * para descobrir quais 'Songs' pertencem a qual 'Playlist'.
     */
    @Relation(
            parentColumn = "playlistId",
            entityColumn = "dataPath",
            associateBy = @Junction(PlaylistSongCrossRef.class)
    )
    // Este campo será preenchido pelo Room com a lista de todas as músicas associadas à 'playlist' acima.
    public List<Song> songs;
}