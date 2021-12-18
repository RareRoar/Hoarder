import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.photos.PhotoSizes;
import com.vk.api.sdk.objects.photos.PhotoSizesType;
import com.vk.api.sdk.objects.wall.WallpostAttachment;
import com.vk.api.sdk.objects.wall.WallpostAttachmentType;
import com.vk.api.sdk.objects.wall.WallpostFull;
import com.vk.api.sdk.objects.wall.responses.GetResponse;
import com.vk.api.sdk.queries.utils.UtilsResolveScreenNameQuery;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.PredicateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class Loader {

    private static void saveImage(URI uri, String sourceName, int suffixIndex) {
        try {
            String extension = getExtensionFromUrl(uri.toASCIIString());
            BufferedImage image = ImageIO.read(uri.toURL());
            File file = new File(System.getProperty("user.dir") +
                    String.format("/%s/%s_%d.%s", sourceName, sourceName, suffixIndex, extension));
            ImageIO.write(image, extension, file);
        }
        catch (IllegalArgumentException ex) {
            logger.error("Extension error " + ex.getMessage());
        }
        catch (MalformedURLException ex) {
            logger.error("URI error " + ex.getMessage());
        }
        catch (IOException ex) {
            logger.error("File IO error " + ex.getMessage());
        }
    }

    private static String getExtensionFromUrl(String url) throws IllegalArgumentException {
        for (String imageFormat : imageFormats) {
            if (url.contains(String.format(".%s?size=", imageFormat))) {
                return imageFormat;
            }
        }
        throw new IllegalArgumentException("Unknown extension.");
    }

    private static Integer getTypePriority(PhotoSizesType type) throws IllegalArgumentException {
        if (type == PhotoSizesType.W) {
            return 10;
        } else if (type == PhotoSizesType.Z) {
            return 9;
        } else if (type == PhotoSizesType.Y) {
            return 8;
        } else if (type == PhotoSizesType.X) {
            return 7;
        } else if (type == PhotoSizesType.M) {
            return 6;
        } else if (type == PhotoSizesType.S) {
            return 5;
        } else if (type == PhotoSizesType.R) {
            return 4;
        } else if (type == PhotoSizesType.Q) {
            return 3;
        } else if (type == PhotoSizesType.P) {
            return 2;
        } else if (type == PhotoSizesType.O) {
            return 1;
        } else throw new IllegalArgumentException("Unknown photo size type.");

    }

    private static PhotoSizes getBestQualitySize(List<PhotoSizes> sizesLists) {
        ArrayList<Integer> priorList = new ArrayList<>();
        for (PhotoSizes photoSize : sizesLists) {
            priorList.add(getTypePriority(photoSize.getType()));
        }
        Integer highestPrior = Collections.max(priorList);
        int priorIndex = priorList.indexOf(highestPrior);
        return sizesLists.get(priorIndex);
    }

    private static void print(Object obj) {
        System.out.println(obj);
    }

    private static VkApiClient getApiClient() {
        TransportClient transportClient = HttpTransportClient.getInstance();
        return new VkApiClient(transportClient);
    }

    private static final Logger logger = LogManager.getLogger(Loader.class);

    private static final ArrayList<String> imageFormats = new ArrayList<>() {
        {
            add("jpg");
            add("jpeg");
            add("png");
            add("gif");
            add("webp");
            add("tiff");
            add("bmp");
        }
    };

    private static final int userId = 152748880;
    private static final String accessToken = "329d75c48182e6d83ed7130fbe8b2fd06e33c5b6238" +
            "c6e6d14bfb8ad3f1174bb13f26cada9e1cd03f9d14";

    private static final int pauseMs = 334;
    private static final int maxPostCount = 100;

    // ARGS ARGS ARGS ARGS ARGS ARGS ARGS ARGS ARGS ARGS ARGS ARGS ARGS
    private static final String groupName = "enemy_org";
    // ARGS ARGS ARGS ARGS ARGS ARGS ARGS ARGS ARGS ARGS ARGS ARGS ARGS

    private static final int threshold = 10000;

    public static void main(String[] args) {

        VkApiClient vk = getApiClient();
        logger.info("Created VK API client.");

        UserActor actor = new UserActor(userId, accessToken);
        logger.info("Created Hoarder App user actor.");

        Integer ownerId;
        try {
            ownerId = -vk.utils()
                    .resolveScreenName(actor, groupName)
                    .execute()
                    .getObjectId();
            logger.info(String.format("Got ownerId:%d from groupName:%s.", ownerId, groupName));
        }
        catch (ApiException ex) {
            logger.error("API error " + ex.getMessage());
            logger.error("Shutdown.");
            return;
        }
        catch (ClientException ex) {
            logger.error("Client error " + ex.getMessage());
            logger.error("Shutdown.");
            return;
        }

        File saveDirectory = new File(System.getProperty("user.dir") + String.format("/%s", groupName));
        if (!saveDirectory.exists()) {
            if (!saveDirectory.mkdir()) {
                logger.error("Cannot create directory " +
                        System.getProperty("user.dir") + String.format("/%s", groupName));
                logger.error("Shutdown");
                return;
            }
        }

        int suffixIndex = 0;
        int globalPostCount = 0;
        int postCount = -1;
        do {
            if (threshold < suffixIndex) {
                logger.info("Threshold is reached, stopping.");
                return;
            }
            List<WallpostFull> posts;
            try {
                    posts = vk.wall()
                            .get(actor)
                            .ownerId(ownerId)
                            .offset(globalPostCount)
                            .count(maxPostCount)
                            .execute()
                            .getItems();
            CollectionUtils.filter(posts, PredicateUtils.notNullPredicate());
            }
            catch (ApiException ex) {
                logger.error("API error " + ex.getMessage());
                logger.error("Shutdown.");
                return;
            }
            catch (ClientException ex) {
                logger.error("Client error " + ex.getMessage());
                logger.error("Shutdown.");
                return;
            }

            postCount = posts.size();
            globalPostCount += postCount;
            logger.info(String.format("Starting %d posts reading.", postCount));

            for (WallpostFull item : posts) {
                List<WallpostAttachment> attachments = item.getAttachments();
                if (attachments != null) {
                    CollectionUtils.filter(attachments, PredicateUtils.notNullPredicate());
                    for (WallpostAttachment attachment : attachments) {
                        if (attachment != null && attachment.getType() == WallpostAttachmentType.PHOTO) {
                            PhotoSizes bestPhoto = getBestQualitySize(attachment.getPhoto().getSizes());
                            URI uri = bestPhoto.getUrl();
                            logger.debug(String.format("Got #%d image URI.", suffixIndex));

                            saveImage(uri, groupName, suffixIndex++);
                            logger.debug(String.format("Saved #%d image.", suffixIndex));
                        }
                    }
                }
            }
            logger.info(String.format("Read %d posts.", postCount));

            logger.debug("Pausing.");
            try {
                Thread.sleep(pauseMs);
            }
            catch (InterruptedException ex) {
                logger.error("Interruption: " + ex.getMessage());
                logger.error("Shutdown.");
                return;
            }
            logger.debug("Awakened.");

        } while (postCount == 100);

        logger.info(String.format("Reading is complete. Read %d posts. Saved %d images", globalPostCount, suffixIndex));
        
        List<PhotoAlbumFull> albums;
        Integer albumCount = 1;
        try {
            albums = vk.photos().getAlbums(actor).ownerId(ownerId).execute().getItems();
            Thread.sleep(pauseMs);
            for (PhotoAlbumFull album : albums) {
                if (album.getSize() == 0) {
                    albumCount++;
                    logger.info("Skipping empty album " + album.getTitle());
                    continue;
                }
                String fixedAlbumTitle = fixFilename(album.getTitle());
                logger.info("Reading album " + album.getTitle());
                Integer offset = 0;
                Integer realCount = 1;
                do {
                    List<Photo> photos = vk.photos().
                            get(actor).
                            ownerId(ownerId).
                            albumId(album.getId().toString()).
                            rev(false).
                            offset(offset).
                            count(1000).execute().getItems();
                    Thread.sleep(pauseMs);
                    offset = photos.size();
                    for(Photo photo : photos) {
                        List<PhotoSizes> photoSizes = photo.getSizes();
                        PhotoSizes bestPhoto = getBestQualitySize(photoSizes);
                        URI uri = bestPhoto.getUrl();
                        File savePhotoDirectory = new File(System.getProperty("user.dir") + String.format("/%s", groupName) + String.format("/%s", fixedAlbumTitle));
                        if (!savePhotoDirectory.exists()) {
                            if (!savePhotoDirectory.mkdir()) {
                                logger.error("Cannot create directory " +
                                        System.getProperty("user.dir") + String.format("/%s", groupName) + String.format("/%s", fixedAlbumTitle));
                                logger.error("Shutdown");
                                return;
                            }
                        }
                        fixedAlbumTitle = fixedAlbumTitle.strip();
                        saveImage(uri, groupName + "/" + fixedAlbumTitle, fixedAlbumTitle, realCount++);

                    }
                }
                while (offset == 0);
                try (PrintWriter out = new PrintWriter(groupName + "/" + fixedAlbumTitle + "/description.txt")) {
                    out.println(album.getDescription());
                } catch (FileNotFoundException e) {
                    System.out.println("Cannot save description.");
                }
                logger.info(String.format("Saved '%s' album.", album.getTitle()));
                logger.info(String.format("Saved %d/%d", albumCount++, albums.size()));

            }
        }
        catch (ApiException ex) {
            logger.error("API error " + ex.getMessage());
            logger.error("Shutdown.");
            return;
        }
        catch (ClientException ex) {
            logger.error("Client error " + ex.getMessage());
            logger.error("Shutdown.");
            return;
        }
        catch (InterruptedException ex) {
            logger.error("Interruption: " + ex.getMessage());
            logger.error("Shutdown.");
            return;
        }
        System.out.println("Finished, darling<3");
    }
}
