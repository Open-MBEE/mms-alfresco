package gov.nasa.jpl.view_repo.util;

import com.hazelcast.core.ItemEvent;
import com.hazelcast.core.ItemListener;
import gov.nasa.jpl.view_repo.util.tasks.BranchTask;
import org.apache.log4j.Logger;

public class QueueConsumer implements ItemListener {
    static Logger logger = Logger.getLogger(QueueConsumer.class);

    public void itemAdded(ItemEvent item) {
        logger.info("Item added = " + item.getItem());
        BranchTask branch = (BranchTask) item.getItem();
        branch.call();
    }

    public void itemRemoved(ItemEvent item) {
        logger.info("Item removed = " + item.getItem());
    }
}
