//
//  MNBase64.java
//  MultiNet client
//
//  Copyright 2010 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core;

final class MNBase64
 {
  public static String encode (byte[] data)
   {
    int dataSize = data.length;

    StringBuilder result = new StringBuilder((dataSize + IN_BLOCK_SIZE - 1) /
                                               IN_BLOCK_SIZE * OUT_BLOCK_SIZE);

    int offset = 0;
    int b0;
    int b1;
    int b2;

    while (dataSize >= IN_BLOCK_SIZE)
     {
      b0 = data[offset    ] & 0xFF;
      b1 = data[offset + 1] & 0xFF;
      b2 = data[offset + 2] & 0xFF;

      result.append(ENCODING_DICT[b0 >>> 2]);
      result.append(ENCODING_DICT[((b0 & 0x03) << 4) | (b1 >>> 4)]);
      result.append(ENCODING_DICT[((b1 & 0x0F) << 2) | (b2 >>> 6)]);
      result.append(ENCODING_DICT[b2 & 0x3F]);

      offset   += IN_BLOCK_SIZE;
      dataSize -= IN_BLOCK_SIZE;
     }

    if      (dataSize == 1)
     {
      b0 = data[offset] & 0xFF;

      result.append(ENCODING_DICT[b0 >>> 2]);
      result.append(ENCODING_DICT[(b0 & 0x03) << 4]);
      result.append(PADDING_CHAR);
      result.append(PADDING_CHAR);
     }
    else if (dataSize == 2)
     {
      b0 = data[offset    ] & 0xFF;
      b1 = data[offset + 1] & 0xFF;

      result.append(ENCODING_DICT[b0 >>> 2]);
      result.append(ENCODING_DICT[((b0 & 0x03) << 4) | (b1 >>> 4)]);
      result.append(ENCODING_DICT[(b1 & 0x0F) << 2]);
      result.append(PADDING_CHAR);
     }

    return result.toString();
   }

  private static final int IN_BLOCK_SIZE  = 3;
  private static final int OUT_BLOCK_SIZE = 4;

  private static final char PADDING_CHAR= '=';

  private static final char[] ENCODING_DICT =
   {
    'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H',
    'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
    'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
    'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f',
    'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
    'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
    'w', 'x', 'y', 'z', '0', '1', '2', '3',
    '4', '5', '6', '7', '8', '9', '+', '/'
   };
 }

