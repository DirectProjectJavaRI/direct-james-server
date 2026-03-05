package org.nhindirect.james.server.spring;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Collection;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.james.GuiceJamesServer;
import org.apache.james.JamesServerMain;
import org.apache.james.NaiveDelegationStoreModule;
import org.apache.james.data.UsersRepositoryModuleChooser;
import org.apache.james.filesystem.api.FileSystem;
import org.apache.james.modules.MailboxModule;
import org.apache.james.modules.MailetProcessingModule;
import org.apache.james.modules.data.JPADataModule;
import org.apache.james.modules.data.JPAUsersRepositoryModule;
import org.apache.james.modules.data.SieveJPARepositoryModules;
import org.apache.james.modules.queue.activemq.ActiveMQQueueModule;
import org.apache.james.modules.mailbox.DefaultEventModule;
import org.apache.james.modules.mailbox.JPAMailboxModule;
import org.apache.james.modules.mailbox.LuceneSearchMailboxModule;
import org.apache.james.modules.mailbox.MemoryDeadLetterModule;
import org.apache.james.modules.protocols.IMAPServerModule;
import org.apache.james.modules.protocols.LMTPServerModule;
import org.apache.james.modules.protocols.ManageSieveServerModule;
import org.apache.james.modules.protocols.POP3ServerModule;
import org.apache.james.modules.protocols.ProtocolHandlerModule;
import org.apache.james.modules.protocols.SMTPServerModule;
import org.apache.james.modules.server.DataRoutesModules;
import org.apache.james.modules.server.DefaultProcessorsConfigurationProviderModule;
import org.apache.james.modules.server.InconsistencyQuotasSolvingRoutesModule;
import org.apache.james.modules.server.MailQueueRoutesModule;
import org.apache.james.modules.server.MailRepositoriesRoutesModule;
import org.apache.james.modules.server.MailboxRoutesModule;
import org.apache.james.modules.server.NoJwtModule;
import org.apache.james.modules.server.RawPostDequeueDecoratorModule;
import org.apache.james.modules.server.ReIndexingModule;
import org.apache.james.modules.server.SieveRoutesModule;
import org.apache.james.modules.server.TaskManagerModule;
import org.apache.james.modules.server.WebAdminReIndexingTaskSerializationModule;
import org.apache.james.modules.server.WebAdminServerModule;
import org.apache.james.server.core.JamesServerResourceLoader;
import org.apache.james.server.core.configuration.Configuration.Basic;
import org.apache.james.server.core.configuration.Configuration.ConfigurationPath;
import org.apache.james.server.core.configuration.FileConfigurationProvider;
import org.apache.james.server.core.filesystem.FileSystemImpl;
import org.nhind.config.rest.AddressService;
import org.nhind.config.rest.DomainService;
import org.nhindirect.config.model.Domain;
import org.nhindirect.james.server.modules.CustomLuceneSearchMailboxModule;
import org.nhindirect.james.server.modules.RESTDataServiceModule;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.inject.Module;
import com.google.inject.util.Modules;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class JamesServerConfig
{
	public static final String DEFAULT_MAILET_CONFIG = "/properties/mailetcontainer.xml";
	public static final String DEFAULT_IMAP_CONFIG = "/properties/imapserver.xml";
	public static final String DEFAULT_POP3_CONFIG = "/properties/pop3server.xml";
	public static final String DEFAULT_SMTP_CONFIG = "/properties/smtpserver.xml";
	public static final String DEFAULT_KEYSTORE = "/properties/keystore";
	
	/*
	 * Data base parameters
	 */
	@Value("${spring.datasource.driver-class-name}")
	protected String driverClassName;

	@Value("${spring.datasource.url}")
	protected String datasourceUrl;
	
	@Value("${spring.datasource.username}")
	protected String datasourceUserName;
	
	@Value("${spring.datasource.password}")
	protected String datasourcePassword;	
	
	@Value("${spring.datasource.adapter}")
	protected String datasourceAdapter;
	
	@Value("${spring.datasource.streaming}")
	protected String datasourceStreaming;
	
	/*
	 * Web admin parameters
	 */
	@Value("${james.server.webadmin.enabled:true}")
	protected String enableWebAdmin;
	
	@Value("${james.server.webadmin.port:8080}")
	protected String webAdminPort;
	
	@Value("${james.server.webadmin.username}")
	protected String webAdminUser;
	
	@Value("${james.server.webadmin.password}")
	protected String webAdminPassword;
	
	@Value("${james.server.webadmin.https.tlsenabled:false}")
	protected String webadminTlsEnabled;
	
	@Value("${james.server.webadmin.https.keystore:}")
	protected String webAdminKeystore;
	
	@Value("${james.server.webadmin.https.keystorePassword:}")
	protected String webAdminKeystorePassword;
	
	@Value("${james.server.webadmin.https.trust.keystore:}")
	protected String webAdminTrustKeystore;
	
	@Value("${james.server.webadmin.https.trust.keystorePassword:}")
	protected String webAdminTrustKeystorePassword;
	
	/*
	 * IMAP Settings
	 */
	@Value("${james.server.imap.bind:0.0.0.0}")
	protected String imapBind;
	
	@Value("${james.server.imap.port:1143}")
	protected String imapPort;
	
	@Value("${james.server.imap.sockettls:false}")
	protected String imapSocketTLS;
	
	@Value("${james.server.imap.starttls:true}")
	protected String imapStartTLS;
	
	@Value("${james.server.imap.keystore:}")
	protected String imapKeyStore;
	
	@Value("${james.server.imap.keystorePassword:1kingpuff}")
	protected String imapKeyStorePassword;
	
	/*
	 * POP3 Settings
	 */
	@Value("${james.server.pop3.bind:0.0.0.0}")
	protected String pop3Bind;
	
	@Value("${james.server.pop3.port:1110}")
	protected String pop3Port;
	
	@Value("${james.server.pop3.sockettls:false}")
	protected String pop3SocketTLS;
	
	@Value("${james.server.pop3.starttls:true}")
	protected String pop3StartTLS;
	
	@Value("${james.server.pop3.keystore:}")
	protected String pop3KeyStore;
	
	@Value("${james.server.pop3.keystorePassword:1kingpuff}")
	protected String pop3KeyStorePassword;
	
	/*
	 * SMTP Settings
	 */
	@Value("${james.server.smtp.bind:0.0.0.0}")
	protected String smtpBind;
	
	@Value("${james.server.smtp.port:1587}")
	protected String smtpPort;
	
	@Value("${james.server.smtp.sockettls:false}")
	protected String smtpSocketTLS;
	
	@Value("${james.server.smtp.starttls:true}")
	protected String smtpStartTLS;
	
	@Value("${james.server.smtp.keystore:}")
	protected String smtpKeyStore;
	
	@Value("${james.server.smtp.keystorePassword:1kingpuff}")
	protected String smtpKeyStorePassword;
	
	@Value("${james.server.smtp.autoAddresses:}")
	protected String smtpAuthAddresses;
	
	/*
	 * Custom configuration files
	 */
	@Value("${james.server.config.mailet.configFile:}")
	protected String mailetConfigFile;
	
	@Value("${james.server.config.imap.configFile:}")
	protected String imapConfigFile;
	
	@Value("${james.server.config.pop3.configFile:}")
	protected String pop3ConfigFile;
	
	@Value("${james.server.config.smtp.configFile:}")
	protected String smtpConfigFile;
	
	@Autowired
	protected DomainService domService;

	public static final Module PROTOCOLS;
	public static final Module JPA_SERVER_MODULE;
	public static final Module JPA_MODULE_AGGREGATE;
	public static final Module WEBADMIN;

	static 
	{	
		
	    WEBADMIN = Modules.combine(
	            new WebAdminServerModule(),
	            new DataRoutesModules(),
	            new InconsistencyQuotasSolvingRoutesModule(),
	            new MailboxRoutesModule(),
	            new MailQueueRoutesModule(),
	            new MailRepositoriesRoutesModule(),
	            new ReIndexingModule(),
	            new SieveRoutesModule(),
	            new WebAdminReIndexingTaskSerializationModule());
        
	    PROTOCOLS = Modules.combine(
	            new IMAPServerModule(),
	            new LMTPServerModule(),
	            new ManageSieveServerModule(),
	            new POP3ServerModule(),
	            new ProtocolHandlerModule(),
	            new SMTPServerModule(),
	            WEBADMIN);

	    JPA_SERVER_MODULE = Modules.combine(
	            new ActiveMQQueueModule(),
	            new NaiveDelegationStoreModule(),
	            new DefaultProcessorsConfigurationProviderModule(),
	            new JPADataModule(),
	            new JPAMailboxModule(),
	            new MailboxModule(),
	            new CustomLuceneSearchMailboxModule(),
	            new NoJwtModule(),
	            new RawPostDequeueDecoratorModule(),
	            new SieveJPARepositoryModules(),
	            new DefaultEventModule(),
	            new TaskManagerModule(),
	            new MemoryDeadLetterModule());

	    JPA_MODULE_AGGREGATE = Modules.combine(
	            new MailetProcessingModule(), JPA_SERVER_MODULE, PROTOCOLS);
	
	}
	
	@Bean
	@ConditionalOnMissingBean
	public GuiceJamesServer jamesServer(DomainService domService, AddressService addrService) throws Exception
	{
		writeJPAConfig();
		
		writeWebAdminConfig();
		
		writeDomainListConfig();
		
		writeMailetConfig();
		
		writeIMAPConfig();
		
		writePOP3Config();
		
		writeSMTPConfig();
		
		writeUserRepositoryConfig();		
		
		final org.apache.james.server.core.configuration.Configuration configuration = 
				org.apache.james.server.core.configuration.Configuration.builder().workingDirectory(".").build();
		
		ConfigurationPath configurationPath = new ConfigurationPath(FileSystem.FILE_PROTOCOL_AND_CONF);
		
		JamesServerResourceLoader directories = new JamesServerResourceLoader(".");
		FileSystemImpl fileSystem = new FileSystemImpl(directories);
        FileConfigurationProvider configurationProvider = new FileConfigurationProvider(fileSystem, Basic.builder()
                .configurationPath(configurationPath)
                .workingDirectory(directories.getRootDirectory())
                .build());
		
		//final GuiceJamesServer server = GuiceJamesServer.forConfiguration(configuration)
		//		.combineWith(new Module[]{JPA_MODULE_AGGREGATE, new RESTDataServiceModule(domService, addrService)});
		
        
		final GuiceJamesServer server =  GuiceJamesServer.forConfiguration(configuration)
                .combineWith(JPA_MODULE_AGGREGATE, new RESTDataServiceModule(domService, addrService))
                .combineWith(new UsersRepositoryModuleChooser(new JPAUsersRepositoryModule())
                    .chooseModules(UsersRepositoryModuleChooser.Implementation.parse(configurationProvider)));
        
		
		
		JamesServerMain.main(server);
		
		return server;
	}
	
	protected void writeJPAConfig() throws Exception
	{
		final File file = new File("conf/james-database.properties");
		
		String dbPropString = IOUtils.resourceToString("/properties/james-database.properties", Charset.defaultCharset());
		dbPropString = dbPropString.replace("${driver}", this.driverClassName);
		dbPropString = dbPropString.replace("${url}", this.datasourceUrl);
		dbPropString = dbPropString.replace("${username}", this.datasourceUserName);
		dbPropString = dbPropString.replace("${password}", this.datasourcePassword);
		dbPropString = dbPropString.replace("${dbadapter}", this.datasourceAdapter);
		
		dbPropString = dbPropString.replace("${streaming}", this.datasourceStreaming);
		
		FileUtils.write(file, dbPropString, Charset.defaultCharset());
	}
	
	protected void writeWebAdminConfig() throws Exception
	{
		final File file = new File("conf/webadmin.properties");
		
		String webAdminString = IOUtils.resourceToString("/properties/webadmin.properties", Charset.defaultCharset());
		webAdminString = webAdminString.replace("${enabled}", this.enableWebAdmin);
		webAdminString = webAdminString.replace("${port}", this.webAdminPort);
		webAdminString = webAdminString.replace("${username}", this.webAdminUser);
		webAdminString = webAdminString.replace("${password}", this.webAdminPassword);
		webAdminString = webAdminString.replace("${httpsEnabled}", this.webadminTlsEnabled);
		webAdminString = webAdminString.replace("${keystore}", this.webAdminKeystore);
		webAdminString = webAdminString.replace("${keystorePassword}", this.webAdminKeystorePassword);
		webAdminString = webAdminString.replace("${trustKeystore}", this.webAdminTrustKeystore);
		webAdminString = webAdminString.replace("${trustPassword}", this.webAdminTrustKeystorePassword);

		
		FileUtils.write(file, webAdminString, Charset.defaultCharset());
	}
	
	protected void writeUserRepositoryConfig() throws Exception
	{
		final File file = new File("conf/usersrepository.xml");
		
		String userRepositoryXML = IOUtils.resourceToString("/properties/userrepository.xml", Charset.defaultCharset());
		
		FileUtils.write(file, userRepositoryXML, Charset.defaultCharset());
	}
	
	protected void writeDomainListConfig() throws Exception
	{
		final File file = new File("conf/domainlist.xml");
		
		String domainlistXML = IOUtils.resourceToString("/properties/domainlist.xml", Charset.defaultCharset());
		final StringBuilder domListBuilder = new StringBuilder();
		
		Collection<Domain> domains = domService.searchDomains("", null);
		if (domains.isEmpty())
		{
			log.warn("No domains defined.  A default list will be injected by James.");
			return;
		}
		for (Domain domain : domains)
		{
			domListBuilder.append("<domain>").append(domain.getDomainName()).append("</domain>\r\n");
		}
		
		domainlistXML = domainlistXML.replace("${domainnames}", domListBuilder.toString());
		// just use the first in the list for the default damain
		domainlistXML = domainlistXML.replace("${defaultdomain}", domains.iterator().next().getDomainName());
		
		FileUtils.write(file, domainlistXML, Charset.defaultCharset());
	}
	
	protected void writeMailetConfig() throws Exception
	{
		/*
		 * Mailet config
		 */
		File writeFile = new File("conf/mailetcontainer.xml");
		byte[] content = (StringUtils.isEmpty(mailetConfigFile)) ? IOUtils.resourceToByteArray(DEFAULT_MAILET_CONFIG) : FileUtils.readFileToByteArray(new File(mailetConfigFile));
		
		
		
		FileUtils.writeByteArrayToFile(writeFile, content);
		
		
	}
	
	protected void writeIMAPConfig() throws Exception
	{
		/*
		 * IMAP config
		 */
		final File configFile = new File("conf/imapserver.xml");
		String content = (StringUtils.isEmpty(imapConfigFile)) ? IOUtils.resourceToString(DEFAULT_IMAP_CONFIG, Charset.defaultCharset()) : FileUtils.readFileToString(new File(imapConfigFile), Charset.defaultCharset());
		
		content = content.replace("${bind}", this.imapBind);
		content = content.replace("${port}", this.imapPort);
		content = content.replace("${socketTLS}", this.imapSocketTLS);
		content = content.replace("${startTLS}", this.imapStartTLS);		
		content = content.replace("${keystorePassword}", this.imapKeyStorePassword);
		
		FileUtils.write(configFile, content, Charset.defaultCharset());
		
		final File keystoreFile = new File("conf/keystore");
		byte[] keyStoreContent = (StringUtils.isEmpty(imapKeyStore)) ? IOUtils.resourceToByteArray(DEFAULT_KEYSTORE) : FileUtils.readFileToByteArray(new File(imapKeyStore));

		FileUtils.writeByteArrayToFile(keystoreFile, keyStoreContent);
	}
	
	protected void writePOP3Config() throws Exception
	{
		/*
		 * POP3 config
		 */
		final File configFile = new File("conf/pop3server.xml");
		String content = (StringUtils.isEmpty(pop3ConfigFile)) ? IOUtils.resourceToString(DEFAULT_POP3_CONFIG, Charset.defaultCharset()) : FileUtils.readFileToString(new File(pop3ConfigFile), Charset.defaultCharset());
		
		content = content.replace("${bind}", this.pop3Bind);
		content = content.replace("${port}", this.pop3Port);
		content = content.replace("${socketTLS}", this.pop3SocketTLS);
		content = content.replace("${startTLS}", this.pop3StartTLS);		
		content = content.replace("${keystorePassword}", this.pop3KeyStorePassword);
		
		FileUtils.write(configFile, content, Charset.defaultCharset());
		
		final File keystoreFile = new File("conf/keystore");
		byte[] keyStoreContent = (StringUtils.isEmpty(pop3KeyStore)) ? IOUtils.resourceToByteArray(DEFAULT_KEYSTORE) : FileUtils.readFileToByteArray(new File(pop3KeyStore));
		FileUtils.writeByteArrayToFile(keystoreFile, keyStoreContent);
	}
	
	protected void writeSMTPConfig() throws Exception
	{
		/*
		 * SMTP config
		 */
		final File configFile = new File("conf/smtpserver.xml");
		String content = (StringUtils.isEmpty(smtpConfigFile)) ? IOUtils.resourceToString(DEFAULT_SMTP_CONFIG, Charset.defaultCharset()) : FileUtils.readFileToString(new File(smtpConfigFile), Charset.defaultCharset());
		
		content = content.replace("${bind}", this.smtpBind);
		content = content.replace("${port}", this.smtpPort);
		content = content.replace("${socketTLS}", this.smtpSocketTLS);
		content = content.replace("${startTLS}", this.smtpStartTLS);		
		content = content.replace("${keystorePassword}", this.smtpKeyStorePassword);
		content = content.replace("${authAddresses}", this.smtpAuthAddresses);
		
		FileUtils.write(configFile, content, Charset.defaultCharset());
		
		final File keystoreFile = new File("conf/keystore");
		byte[] keyStoreContent = (StringUtils.isEmpty(smtpKeyStore)) ? IOUtils.resourceToByteArray(DEFAULT_KEYSTORE) : FileUtils.readFileToByteArray(new File(smtpKeyStore));
		FileUtils.writeByteArrayToFile(keystoreFile, keyStoreContent);
	}
}
