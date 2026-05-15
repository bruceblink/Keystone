#!/usr/bin/env python3
"""
Generate an RSA key pair in the exact Base64 formats Keystone expects.

Private key:
  PKCS#8 DER -> Base64

Public key:
  SubjectPublicKeyInfo (X.509) DER -> Base64
"""

from __future__ import annotations

import argparse
import base64
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import rsa


def generate_keypair(bits: int) -> tuple[str, str]:
    private_key = rsa.generate_private_key(public_exponent=65537, key_size=bits)
    public_key = private_key.public_key()

    private_key_der = private_key.private_bytes(
        encoding=serialization.Encoding.DER,
        format=serialization.PrivateFormat.PKCS8,
        encryption_algorithm=serialization.NoEncryption(),
    )
    public_key_der = public_key.public_bytes(
        encoding=serialization.Encoding.DER,
        format=serialization.PublicFormat.SubjectPublicKeyInfo,
    )

    private_key_base64 = base64.b64encode(private_key_der).decode("ascii")
    public_key_base64 = base64.b64encode(public_key_der).decode("ascii")
    return private_key_base64, public_key_base64


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Generate an RSA key pair for Keystone login password encryption."
    )
    parser.add_argument(
        "--bits",
        type=int,
        default=2048,
        choices=(2048, 3072, 4096),
        help="RSA key size. Default: 2048",
    )
    args = parser.parse_args()

    private_key, public_key = generate_keypair(args.bits)

    print("# Keystone RSA key pair")
    print(f"# key_size={args.bits}")
    print("KEYSTONE_RSA_PRIVATE_KEY=" + private_key)
    print("KEYSTONE_RSA_PUBLIC_KEY=" + public_key)
    print()
    print("# Raw values")
    print(private_key)
    print(public_key)


if __name__ == "__main__":
    main()
