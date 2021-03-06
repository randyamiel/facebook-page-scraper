package common;

import db.DbManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class Config
{
    public static final String baseUrl = "https://graph.facebook.com/v2.6";
    public static final int dayInMillis = 86400000;
    public static final int hourInMillis = 3600000;
    public static final int minuteInMillis = 60000;

    private static List<String> accessTokens = new ArrayList<String>();
    private static int atPoolPointer = 0;
    public static List<String> pages = new ArrayList<String>();

    /* data collector specific parameters */
    public static int numOfScrapes;
    public static String baseDir;
    public static String since;
    public static String until;
    public static boolean collectComments;
    public static boolean collectCommentReplies;
    public static boolean collectLikes;
    public static boolean scrapeHistory;

    /* database connection parameters */
    public static String dbUrl;
    public static String dbUser;
    public static String dbPass;

    public static boolean insertCommentReplies;

    /* only used by stats collector */
    public static int statsDepth;
    public static int statsInterval;
    public static boolean statsHistory;

    /* these dirs will be created if does not exist */
    public static String downloadDir;
    public static String archiveDir;

    /* optional */
    public static int delay;

    public static boolean exitWhenFetchFails;

    public static int fetchRetries;

    /* used by pseudo tagger */
    public static String tagTable;
    public static List<Integer> excludeCodes = new ArrayList<Integer>();

    public static String filterPostsSql;

    private static void init()
    {
        Properties properties = new Properties();
        InputStream inputStream = null;
        try
        {
            if(new File("config.properties").exists())
            {
                inputStream = new FileInputStream("config.properties");
            }
            if(null == inputStream)
            {
                inputStream = new FileInputStream(System.getProperty("user.home") + "/facebook/config.properties");
            }
            if(null == inputStream)
            {
                System.err.println("Could not find config.properties. Searched in base directory and " + System.getProperty("user.home") + "/fb-page-scraper");
                System.exit(0);
            }

            properties.load(inputStream);

            if(null != properties.getProperty("accessTokens") && !properties.getProperty("accessTokens").isEmpty())
            {
                accessTokens = Arrays.asList(properties.getProperty("accessTokens").split("\\s*,\\s*"));
            }

            if(null != properties.getProperty("pages") && !properties.getProperty("pages").isEmpty())
            {
                pages = Arrays.asList(properties.getProperty("pages").split("\\s*,\\s*"));
            }

            numOfScrapes =  Integer.parseInt(properties.getProperty("numOfScrapes", "0"));

            baseDir = properties.getProperty("baseDir", System.getProperty("user.home") + "/facebook");
            downloadDir = baseDir + "/download";
            archiveDir = baseDir + "/archive";

            since = properties.getProperty("since");
            until = properties.getProperty("until");
            if(null == Config.until || Config.until.isEmpty() || !Config.until.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}"))
            {
                Config.until = Util.getDateTimeUtc(System.currentTimeMillis());
            }

            collectComments = properties.getProperty("collectComments", "false").toLowerCase().equals("true");
            collectCommentReplies = properties.getProperty("collectCommentReplies", "false").toLowerCase().equals("true");
            collectLikes = properties.getProperty("collectLikes", "false").toLowerCase().equals("true");
            scrapeHistory = properties.getProperty("scrapeHistory", "false").toLowerCase().equals("true");

            dbUrl = properties.getProperty("dbUrl");
            dbUser = properties.getProperty("dbUser");
            dbPass = properties.getProperty("dbPass");

            insertCommentReplies = properties.getProperty("insertCommentReplies", "true").toLowerCase().equals("true");

            statsDepth =  Integer.parseInt(properties.getProperty("statsDepth", "5"));
            statsInterval =  Integer.parseInt(properties.getProperty("statsInterval", "10"));
            statsHistory = properties.getProperty("statsHistory", "true").toLowerCase().equals("true");

            delay = Integer.parseInt(properties.getProperty("delay", "1"));

            exitWhenFetchFails = properties.getProperty("exitWhenFetchFails", "true").toLowerCase().equals("true");

            fetchRetries =  Integer.parseInt(properties.getProperty("fetchRetries", "5"));

            tagTable = properties.getProperty("tagTable");
            if(null != properties.getProperty("excludeCodes"))
            {
                for(String s: properties.getProperty("excludeCodes").split("\\s*,\\s*"))
                {
                    if(s.matches("\\d+"))
                    {
                        excludeCodes.add(Integer.parseInt(s));
                    }
                }
            }

            filterPostsSql = properties.getProperty("filterPostsSql");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            if(null != inputStream)
            {
                try
                {
                    inputStream.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void initDataCollector()
    {
        init();

        if(isFetchConfigValid() && isDateValid() && isBaseDirValid())
        {
            /* create download directory if it does not exist */
            Util.buildPath("download");
        }
    }

    public static void initImageCollector()
    {
        init();

        if(!isFetchConfigValid() || !isBaseDirValid())
        {
            System.exit(0);
        }
    }

    public static void initInserter()
    {
        init();

        if(isBaseDirValid() && isDbConfigValid())
        {
            /* create archive directory if it does not exist */
            Util.buildPath("archive");
        }
        else
        {
            System.exit(0);
        }
    }

    public static void initStats()
    {
        init();

        if(!isFetchConfigValid() || !isDbConfigValid())
        {
            System.exit(0);
        }
    }

    public static void initPseudoTagging()
    {
        init();

        if(isDbConfigValid())
        {
            if(null == tagTable || tagTable.isEmpty())
            {
                System.err.println(Util.getDbDateTimeEst() + " tagTable has no value");
                System.exit(0);
            }
        }
        else
        {
            System.exit(0);
        }
    }

    private static boolean isFetchConfigValid()
    {
        if(accessTokens.size() == 0)
        {
            System.err.println(Util.getDbDateTimeEst() + " accessToken missing");
            return false;
        }

        if(pages == null || pages.size() == 0)
        {
            System.err.println(Util.getDbDateTimeEst() + " pages missing");
            return false;
        }

        return true;
    }

    private static boolean isDateValid()
    {
        if(null == since || since.isEmpty() || !since.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}"))
        {
            System.err.println(Util.getDbDateTimeEst() + " invalid start date (since)");
            return false;
        }

        return true;
    }

    private static boolean isBaseDirValid()
    {
        if(null == baseDir || baseDir.isEmpty())
        {
            System.err.println(Util.getDbDateTimeEst() + " base directory is required");
            return false;
        }

        File tempDir = new File(baseDir);
        if(!tempDir.exists() || !tempDir.isDirectory())
        {
            System.err.println(Util.getDbDateTimeEst() + " invalid base directory " + baseDir);
            return false;
        }

        System.out.println("base directory is " + baseDir);

        return true;
    }

    private static boolean isDbConfigValid()
    {
        if(null == dbUrl || null == dbUser || null == dbPass)
        {
            System.err.println(Util.getDbDateTimeEst() + " database connection parameters are required");
            return false;
        }

        if(!DbManager.isParamsValid())
        {
            System.err.println(Util.getDbDateTimeEst() + " invalid database parameters");
            return false;
        }

        return true;
    }

    public static String getAccessToken()
    {
        // make sure access token pool pointer never goes out of bound
        if(atPoolPointer > 0)
        {
            atPoolPointer = atPoolPointer % accessTokens.size();
        }
        String accessToken = accessTokens.get(atPoolPointer);
        atPoolPointer = ++atPoolPointer % accessTokens.size();
        return accessToken;
    }
}
