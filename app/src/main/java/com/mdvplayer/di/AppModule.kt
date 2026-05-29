package com.mdvplayer.di

import android.content.Context
import coil.ImageLoader
import com.mdvplayer.data.repository.MusicRepositoryImpl
import com.mdvplayer.domain.repository.MusicRepository
import com.mdvplayer.utils.AlbumArtFetcher
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindMusicRepository(impl: MusicRepositoryImpl): MusicRepository

    companion object {

        @Provides
        @Singleton
        @ApplicationScope
        fun provideApplicationScope(): CoroutineScope =
            CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        @Provides
        @Singleton
        fun provideImageLoader(
            @ApplicationContext context: Context
        ): ImageLoader = ImageLoader.Builder(context)
            .components {
                add(AlbumArtFetcher.Factory())
            }
            .build()
    }
}
