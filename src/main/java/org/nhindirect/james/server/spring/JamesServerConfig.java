package org.nhindirect.james.server.spring;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Collection;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.james.GuiceJamesServer;
import org.apache.james.modules.MailboxModule;
import org.apache.james.modules.activemq.ActiveMQQueueModule;
import org.apache.james.modules.data.JPADataModule;
import org.apache.james.modules.data.SieveJPARepositoryModules;
import org.apache.james.modules.mailbox.DefaultEventModule;
import org.apache.james.modules.mailbox.JPAMailboxModule;
import org.apache.james.modules.mailbox.LuceneSearchMailboxModule;
import org.apache.james.modules.protocols.IMAPServerModule;
import org.apache.james.modules.protocols.LMTPServerModule;
import org.apache.james.modules.protocols.ManageSieveServerModule;
import org.apache.james.modules.protocols.POP3ServerModule;
import org.apache.james.modules.protocols.ProtocolHandlerModule;
import org.apache.james.modules.protocols.SMTPServerModule;
import org.apache.james.modules.server.DataRoutesModules;
import org.apache.james.modules.server.DefaultProcessorsConfigurationProviderModule;
import org.apache.james.modules.server.ElasticSearchMetricReporterModule;
import org.apache.james.modules.server.JMXServerModule;
import org.apache.james.modules.server.MailQueueRoutesModule;
import org.apache.james.modules.server.MailRepositoriesRoutesModule;
import org.apache.james.modules.server.MailboxRoutesModule;
import org.apache.james.modules.server.NoJwtModule;
import org.apache.james.modules.server.RawPostDequeueDecoratorModule;
import org.apache.james.modules.server.ReIndexingModule;
import org.apache.james.modules.server.SieveQuotaRoutesModule;
import org.apache.james.modules.server.SwaggerRoutesModule;
import org.apache.james.modules.spamassassin.SpamAssassinListenerModule;
import org.nhind.config.rest.DomainService;
import org.nhindirect.config.model.Domain;
import org.nhindirect.james.server.modules.DirectWebAdminServerModule;
import org.parboiled.common.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.inject.Module;
import com.google.inject.util.Modules;

@Configuration
public class JamesServerConfig
{
	private static final Logger LOGGER = LoggerFactory.getLogger(JamesServerConfig.class);	
	
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
	
	@Value("${james.server.smtp.autoAddresses:127.0.0.0/8}")
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
	            new DirectWebAdminServerModule(),
	            new DataRoutesModules(),
	            new MailboxRoutesModule(),
	            new MailQueueRoutesModule(),
	            new MailRepositoriesRoutesModule(),
	            new SwaggerRoutesModule(),
	            new SieveQuotaRoutesModule(),
	            new ReIndexingModule());		
		
		PROTOCOLS = Modules
				.combine(new Module[]{new IMAPServerModule(), new LMTPServerModule(), new ManageSieveServerModule(),
						new POP3ServerModule(), new ProtocolHandlerModule(), new SMTPServerModule(), WEBADMIN});
		
		JPA_SERVER_MODULE = Modules.combine(new Module[]{new ActiveMQQueueModule(),
				new DefaultProcessorsConfigurationProviderModule(), new ElasticSearchMetricReporterModule(),
				new JPADataModule(), new JPAMailboxModule(), new MailboxModule(), new LuceneSearchMailboxModule(), new NoJwtModule(),
				new RawPostDequeueDecoratorModule(), new SieveJPARepositoryModules(),
				new DefaultEventModule(), new SpamAssassinListenerModule()});
		
		JPA_MODULE_AGGREGATE = Modules.combine(new Module[]{JPA_SERVER_MODULE, PROTOCOLS});
	}
	
	
	@Bean
	@ConditionalOnMissingBean
	public GuiceJamesServer jamesServer() throws Exception
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
		
		final GuiceJamesServer server = GuiceJamesServer.forConfiguration(configuration)
				.combineWith(new Module[]{JPA_MODULE_AGGREGATE, new JMXServerModule()});
		
		server.start();
		
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
		
		FileUtils.writeAllText(dbPropString, file);
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

		
		FileUtils.writeAllText(webAdminString, file);
	}
	
	protected void writeUserRepositoryConfig() throws Exception
	{
		final File file = new File("conf/usersrepository.xml");
		
		String userRepositoryXML = IOUtils.resourceToString("/properties/userrepository.xml", Charset.defaultCharset());
		
		FileUtils.writeAllText(userRepositoryXML, file);
	}
	
	protected void writeDomainListConfig() throws Exception
	{
		final File file = new File("conf/domainlist.xml");
		
		String domainlistXML = IOUtils.resourceToString("/properties/domainlist.xml", Charset.defaultCharset());
		final StringBuilder domListBuilder = new StringBuilder();
		
		Collection<Domain> domains = domService.searchDomains("", null);
		if (domains.isEmpty())
		{
			LOGGER.warn("No domains defined.  A default list will be injected by James.");
			return;
		}
		for (Domain domain : domains)
		{
			domListBuilder.append("<domain>").append(domain.getDomainName()).append("</domain>\r\n");
		}
		
		domainlistXML = domainlistXML.replace("${domainnames}", domListBuilder.toString());
		// just use the first in the list for the default damain
		domainlistXML = domainlistXML.replace("${defaultdomain}", domains.iterator().next().getDomainName());
		
		FileUtils.writeAllText(domainlistXML, file);
	}
	
	protected void writeMailetConfig() throws Exception
	{
		/*
		 * Mailet config
		 */
		File writeFile = new File("conf/mailetcontainer.xml");
		byte[] content = (StringUtils.isEmpty(mailetConfigFile)) ? IOUtils.resourceToByteArray(DEFAULT_MAILET_CONFIG) : FileUtils.readAllBytes(new File(mailetConfigFile));
		FileUtils.writeAllBytes(content, writeFile);
	}
	
	protected void writeIMAPConfig() throws Exception
	{
		/*
		 * IMAP config
		 */
		final File configFile = new File("conf/imapserver.xml");
		String content = (StringUtils.isEmpty(imapConfigFile)) ? IOUtils.resourceToString(DEFAULT_IMAP_CONFIG, Charset.defaultCharset()) : FileUtils.readAllText(new File(imapConfigFile));
		
		content = content.replace("${bind}", this.imapBind);
		content = content.replace("${port}", this.imapPort);
		content = content.replace("${socketTLS}", this.imapSocketTLS);
		content = content.replace("${startTLS}", this.imapStartTLS);		
		content = content.replace("${keystorePassword}", this.imapKeyStorePassword);
		
		FileUtils.writeAllText(content, configFile);
		
		final File keystoreFile = new File("conf/keystore");
		byte[] keyStoreContent = (StringUtils.isEmpty(imapKeyStore)) ? IOUtils.resourceToByteArray(DEFAULT_KEYSTORE) : FileUtils.readAllBytes(new File(imapKeyStore));
		FileUtils.writeAllBytes(keyStoreContent, keystoreFile);
	}
	
	protected void writePOP3Config() throws Exception
	{
		/*
		 * POP3 config
		 */
		final File configFile = new File("conf/pop3server.xml");
		String content = (StringUtils.isEmpty(pop3ConfigFile)) ? IOUtils.resourceToString(DEFAULT_POP3_CONFIG, Charset.defaultCharset()) : FileUtils.readAllText(new File(pop3ConfigFile));
		
		content = content.replace("${bind}", this.pop3Bind);
		content = content.replace("${port}", this.pop3Port);
		content = content.replace("${socketTLS}", this.pop3SocketTLS);
		content = content.replace("${startTLS}", this.pop3StartTLS);		
		content = content.replace("${keystorePassword}", this.pop3KeyStorePassword);
		
		FileUtils.writeAllText(content, configFile);
		
		final File keystoreFile = new File("conf/keystore");
		byte[] keyStoreContent = (StringUtils.isEmpty(pop3KeyStore)) ? IOUtils.resourceToByteArray(DEFAULT_KEYSTORE) : FileUtils.readAllBytes(new File(pop3KeyStore));
		FileUtils.writeAllBytes(keyStoreContent, keystoreFile);
	}
	
	protected void writeSMTPConfig() throws Exception
	{
		/*
		 * SMTP config
		 */
		final File configFile = new File("conf/smtpserver.xml");
		String content = (StringUtils.isEmpty(smtpConfigFile)) ? IOUtils.resourceToString(DEFAULT_SMTP_CONFIG, Charset.defaultCharset()) : FileUtils.readAllText(new File(smtpConfigFile));
		
		content = content.replace("${bind}", this.smtpBind);
		content = content.replace("${port}", this.smtpPort);
		content = content.replace("${socketTLS}", this.smtpSocketTLS);
		content = content.replace("${startTLS}", this.smtpStartTLS);		
		content = content.replace("${keystorePassword}", this.smtpKeyStorePassword);
		content = content.replace("${authAddresses}", this.smtpAuthAddresses);
		
		FileUtils.writeAllText(content, configFile);
		
		final File keystoreFile = new File("conf/keystore");
		byte[] keyStoreContent = (StringUtils.isEmpty(smtpKeyStore)) ? IOUtils.resourceToByteArray(DEFAULT_KEYSTORE) : FileUtils.readAllBytes(new File(smtpKeyStore));
		FileUtils.writeAllBytes(keyStoreContent, keystoreFile);
	}
}
