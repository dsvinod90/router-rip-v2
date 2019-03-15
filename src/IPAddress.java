/**
 * filename: @{@link model.IPAddress}
 * author: @ishanguliani aka ig5859
 *
 * version:     1
 *
 * revision:    1
 *
 * description:
 * A model class to determine IP Address from the network packet
 */

import java.util.ArrayList;
import java.util.List;

public class IPAddress {

    private List<Byte> ipAddressModel = new ArrayList<Byte>();
    private String ipAddress;

    public IPAddress(List<Byte> subList) {
        this.ipAddressModel = subList;
        prepareIPAddress();
    }

    /**
     * Prepare the IP address from the list of bytes obtained
     */
    private void prepareIPAddress() {
        StringBuilder builder = new StringBuilder();
        for( byte elem: ipAddressModel)  {
            builder.append(Helper.BitwiseManager.convertByteToDecimal(elem)).append("");
        }
        this.ipAddress = builder.toString().substring(0, builder.toString().length() - 1);
    }


    @Override
    public String toString() {
        return this.ipAddress;
    }
}