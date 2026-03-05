package org.nhindirect.james.server.modules;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;

import org.apache.james.mailbox.store.search.MessageSearchIndex;

import java.io.File;
import java.io.IOException;

import org.apache.james.events.EventListener;
import org.apache.james.filesystem.api.FileSystem;
import org.apache.james.mailbox.lucene.search.LuceneMessageSearchIndex;
import org.apache.james.mailbox.store.search.ListeningMessageSearchIndex;
import org.apache.james.modules.mailbox.ReIndexingTaskSerializationModule;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class CustomLuceneSearchMailboxModule extends AbstractModule {

	private static final String LUCENE_SUBDIR = "lucene";
	
    @Override
    protected void configure() {
        install(new ReIndexingTaskSerializationModule());

        bind(LuceneMessageSearchIndex.class).in(Scopes.SINGLETON);
        bind(MessageSearchIndex.class).to(LuceneMessageSearchIndex.class);
        bind(ListeningMessageSearchIndex.class).to(LuceneMessageSearchIndex.class);

        Multibinder.newSetBinder(binder(), EventListener.ReactiveGroupEventListener.class)
            .addBinding()
            .to(LuceneMessageSearchIndex.class);
    }

    @Provides
    @Singleton
    Directory provideDirectory(FileSystem fileSystem) throws IOException {

        String filePath = (fileSystem.getBasedir().getAbsolutePath().endsWith(File.separator)) ?
        		fileSystem.getBasedir().getAbsolutePath() + LUCENE_SUBDIR :
        			fileSystem.getBasedir().getAbsolutePath() + File.separator + LUCENE_SUBDIR;
        			
        return FSDirectory.open(new File(filePath));
    }
}