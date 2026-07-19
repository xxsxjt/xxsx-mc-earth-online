#!/usr/bin/env python3
"""Minimal Minecraft RCON client used by deterministic smoke tests."""

from __future__ import annotations

import argparse
import os
import socket
import struct
import sys
from dataclasses import dataclass


MAX_PACKET_BYTES = 10 * 1024 * 1024


@dataclass(frozen=True)
class RconPacket:
    request_id: int
    packet_type: int
    payload: str


def read_exact(sock: socket.socket, size: int) -> bytes:
    chunks: list[bytes] = []
    remaining = size
    while remaining:
        chunk = sock.recv(remaining)
        if not chunk:
            raise ConnectionError("RCON connection closed while receiving a packet")
        chunks.append(chunk)
        remaining -= len(chunk)
    return b"".join(chunks)


def encode_packet(request_id: int, packet_type: int, payload: str) -> bytes:
    encoded = payload.encode("utf-8")
    body = struct.pack("<ii", request_id, packet_type) + encoded + b"\x00\x00"
    return struct.pack("<i", len(body)) + body


def read_packet(sock: socket.socket) -> RconPacket:
    (length,) = struct.unpack("<i", read_exact(sock, 4))
    if length < 10 or length > MAX_PACKET_BYTES:
        raise ValueError(f"Invalid RCON packet length: {length}")

    body = read_exact(sock, length)
    if body[-2:] != b"\x00\x00":
        raise ValueError("RCON packet is missing its two-byte terminator")
    request_id, packet_type = struct.unpack("<ii", body[:8])
    return RconPacket(
        request_id=request_id,
        packet_type=packet_type,
        payload=body[8:-2].decode("utf-8", errors="replace"),
    )


def authenticate(sock: socket.socket, password: str, request_id: int = 101) -> None:
    sock.sendall(encode_packet(request_id, 3, password))
    for _ in range(3):
        response = read_packet(sock)
        if response.request_id == -1:
            raise PermissionError("RCON authentication failed")
        if response.request_id == request_id and response.packet_type == 2:
            return
    raise PermissionError("RCON authentication response was not received")


def execute_command(
    host: str,
    port: int,
    password: str,
    command: str,
    timeout: float,
) -> str:
    with socket.create_connection((host, port), timeout=timeout) as sock:
        sock.settimeout(timeout)
        authenticate(sock, password)
        sock.sendall(encode_packet(102, 2, command))
        try:
            response = read_packet(sock)
        except (ConnectionError, TimeoutError, socket.timeout):
            if command.strip().lower() == "stop":
                return ""
            raise
        if response.request_id != 102:
            raise RuntimeError(
                f"Unexpected RCON response id {response.request_id}; expected 102"
            )
        return response.payload


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", help="Minecraft server command without a leading slash")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=25575)
    parser.add_argument("--timeout", type=float, default=10.0)
    parser.add_argument(
        "--password-env",
        default="MINECRAFT_RCON_PASSWORD",
        help="Environment variable containing the RCON password",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    password = os.environ.get(args.password_env)
    if not password:
        print(f"Missing RCON password environment variable: {args.password_env}", file=sys.stderr)
        return 2
    response = execute_command(args.host, args.port, password, args.command, args.timeout)
    if response:
        print(response.rstrip())
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
