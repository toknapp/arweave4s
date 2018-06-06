# Scripts

## Usage

```shell
$ export ARWEAVE_NODE=my.arweave.node:1984    # for convenience, use -h flag to override
$ ./new-wallet > my-wallet.json
$ ./address my-wallet.json
IcbkRMb1Y3UGrX93XutuTsSGQEDD7BcDlbRX8wyLEWw
$ ./transfer -w wallet-with-tokens.json -q 1 -a IcbkRMb1Y3UGrX93XutuTsSGQEDD7BcDlbRX8wyLEWw
-Dfz-O-Q2bp_OK3Wm7gZ_m7WKwmNnewRh2U7lD9hkz0
$ ./balance -a IcbkRMb1Y3UGrX93XutuTsSGQEDD7BcDlbRX8wyLEWw
1000000000000
```
