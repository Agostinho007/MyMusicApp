package com.dev.mymusicapp.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.dev.mymusicapp.model.Playlist;
import com.dev.mymusicapp.model.PlaylistSongCrossRef;
import com.dev.mymusicapp.model.Song;

/**
 * A anotação @Database diz ao Room que esta classe representa a base de dados da aplicação.
 *
 * entities       Lista de todas as classes de "Entidade" (tabelas) que pertencem a esta base de dados.
 * version        A versão da base de dados. Deve ser incrementada sempre que o esquema (a estrutura das tabelas) muda.
 * exportSchema   Se deve ou não exportar o esquema da base de dados para um ficheiro JSON. É útil para versionamento complexo,
 * mas para este projeto, definimos como 'false' para simplificar.
 */
@Database(entities = {Playlist.class, Song.class, PlaylistSongCrossRef.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    /**
     * Declara um metodo abstrato para cada DAO (Data Access Object).
     * O Room irá gerar automaticamente a implementação deste metodo para nós.
     * Através deste metodo, teremos acesso a todas as operações de base de dados definidas em PlaylistDao.
     * @return Uma instância do nosso PlaylistDao.
     */
    public abstract PlaylistDao playlistDao();

    /**
     * Implementação do padrão Singleton para a nossa base de dados.
     * A palavra-chave 'volatile' garante que a variável INSTANCE seja sempre lida da memória principal,
     * o que é importante para a segurança em ambientes com múltiplas threads.
     * Isto garante que apenas uma única instância da base de dados seja criada e utilizada em toda a aplicação,
     * o que é eficiente e seguro.
     */
    private static volatile AppDatabase INSTANCE;

    /**
     * Metodo estático público que retorna a instância única da base de dados.
     * Se a instância ainda não existir, ela será criada.
     *
     * @param context O contexto da aplicação.
     * @return A instância Singleton de AppDatabase.
     */
    public static AppDatabase getDatabase(final Context context) {
        // Primeira verificação (sem bloqueio) para ver se a instância já foi criada.
        if (INSTANCE == null) {
            // Bloco 'synchronized' para garantir que apenas uma thread pode criar a instância da base de dados de cada vez.
            // Isto previne "race conditions" onde duas threads poderiam tentar criar a base de dados ao mesmo tempo.
            synchronized (AppDatabase.class) {
                // Segunda verificação (dentro do bloqueio) para garantir que outra thread não criou a instância
                // enquanto a primeira esperava para entrar no bloco sincronizado.
                if (INSTANCE == null) {
                    // Se a instância ainda é nula, usamos o Room.databaseBuilder para criar a nossa base de dados.
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "music_database") // O nome do ficheiro da base de dados no dispositivo será "music_database".
                            .build(); // Constrói e retorna a instância da base de dados.
                }
            }
        }
        // Retorna a instância existente ou a recém-criada.
        return INSTANCE;
    }
}